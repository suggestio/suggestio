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
    val resultFut = super.invokeBlock(request, block)
    resultFut map { result =>
      val session0 = result.session(request)
      if (session0.isEmpty) {
        // Не заниматься ковырянием сессии, если она пустая.
        result
      } else {
        val currTstamp = currentTstamp
        session0
          .get(SESSION_TSTAMP_KEY)
          .flatMap { parseTstamp }
          // Уже ясно, что нет таймштампа в сессии. Значит это сессия в старом формате или она была только что выставлена в контроллере. Нужно выставить туда currTstamp.
          .orElse { Some(currentTstamp) }
          // Отфильтровать устаревшие timestamp'ы.
          .filter { isTimestampValid(_, currTstamp) }
          .fold {
            // Таймштамп истёк -- стереть сессию.
            trace("invokeBlock(): Erasing expired session for person " + session0.get(Security.username))
            result.withNewSession
          } { _ =>
            // Есть таймштамп, значит пора залить новый (настоящий) таймштамп в сессию.
            val session1 = withTimestamp(session0, currTstamp)
            result.withSession(session1)
          }
      }
    }
  }
}

