package util.acl

import play.api.mvc._
import scala.concurrent.Future
import play.api.Play.{current, configuration}
import scala.concurrent.duration._
import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.14 17:57
 * Description: Функции для сброса сессии при наступлении таймаута, и соотв.утиль для пролонгации сессии.
 * 2014.feb.06: Из-за добавления в сессию securesocial, csrf-token и т.д. нужно аккуратнее работать с сессией,
 * без использования withNewSession().
 */
object ExpireSession extends PlayMacroLogsImpl {

  import LOGGER._

  /** Ключ с временем в карте сессии. */
  val SESSION_TSTAMP_KEY  = configuration.getString("session.tstamp.key") getOrElse "t"

  /** Время жизни сессии. */
  val SESSION_TTL_SECONDS = configuration.getInt("session.maxAge")
    .map(_ milliseconds)
    .getOrElse(10 minutes)
    .toSeconds

  /** play-флаг session.secure. Нужно для защиты от передачи кук по голому http. */
  val SECURE_SESSION = configuration.getBoolean("session.secure") getOrElse false

  /** Чтобы не делать лишних движений, фиксируем начало времён, вычитая его из всех timestamp'ов. */
  val TSAMP_SUBSTRACT = 1402927907242L

  trace(s"Session tstamp key = $SESSION_TSTAMP_KEY; session ttl = ${SESSION_TTL_SECONDS}s = ${SESSION_TTL_SECONDS / 60} minutes")

  /** Сгенерить текущий timestamp. */
  def currentTstamp = (System.currentTimeMillis() - TSAMP_SUBSTRACT) / 1000L

  /**
   * Добавить timestamp в существующую карты сессии.
   * @param session0 Исходная сессия.
   * @param tstamp Таймштамп -- результат currentTstamp(). Если не указан, то будет вызвана currentTstamp.
   * @return Обновлённая сессия.
   */
  def withTimestamp(session0: Session, tstamp: Long = currentTstamp): Session = {
    session0 + (SESSION_TSTAMP_KEY -> tstamp.toString)
  }

  def parseTstamp(tstampStr: String): Option[Long] = {
    try {
      Some(tstampStr.toLong)
    } catch {
      case ex: Exception =>
        warn(s"invokeBlock(): Failed to parse session timestamp: raw = $tstampStr")
        None
    }
  }

  def isTimestampValid(tstamp: Long, currTstamp: Long = currentTstamp): Boolean = {
    tstamp + SESSION_TTL_SECONDS >= currTstamp
  }
}


import ExpireSession._


/**
 * Трейт, добавляющий в сессию TTL. Добавляется в конце реализации ActionBuilder'а.
 * @tparam R тип реквеста, с которым работаем. Просто форвардится из декларации класса ActionBuilder'а.
 */
trait ExpireSession[R[_]] extends ActionBuilder[R] {
  import LOGGER._

  abstract override def invokeBlock[A](request: Request[A], block: (R[A]) => Future[Result]): Future[Result] = {
    super.invokeBlock(request, block) map { result =>
      val session0 = result.session(request)
      val stkOpt = session0.get(SESSION_TSTAMP_KEY)
      val currTstamp = currentTstamp
      if (stkOpt.isEmpty) {
        // Не заниматься ковырянием сессии, если юзер не залогинен.
        // Уже ясно, что нет таймштампа в сессии. Значит она была только что выставлена в контроллере.
        // Нужно выставить туда currTstamp, если username задан.
        if (session0.data contains Security.username) {
          val session1 = session0 + (SESSION_TSTAMP_KEY -> currTstamp.toString)
          result.withSession(session1)
        } else {
          result
        }

      } else {
        val newTsOpt = stkOpt
          .flatMap { parseTstamp }
          // Отфильтровать устаревшие timestamp'ы.
          .filter { isTimestampValid(_, currTstamp) }
        val session1 = newTsOpt match {
          case None =>
            // Таймштамп истёк -- стереть из сессии таймштамп и username.
            trace("invokeBlock(): Erasing expired session for person " + session0.get(Security.username))
            session0.copy(
              data = session0.data.filterKeys { k =>  !(k == Security.username || k == SESSION_TSTAMP_KEY) }
            )
          case _ =>
            // Есть таймштамп, значит пора залить новый (текущий) таймштамп в сессию.
            withTimestamp(session0, currTstamp)
        }
        result withSession session1
      }
    }
  }
}

