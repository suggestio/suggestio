package util.acl

import models.msession._
import play.api.mvc._
import scala.concurrent.Future
import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.msession.Keys._
import scala.language.higherKinds

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.14 17:57
 * Description: Функции для сброса сессии при наступлении таймаута, и соотв.утиль для пролонгации сессии.
 * 2014.feb.06: Из-за добавления в сессию securesocial, csrf-token и т.д. нужно аккуратнее работать с сессией,
 * без использования withNewSession().
 */
object ExpireSession extends PlayMacroLogsImpl

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
      val tstampOpt = LoginTimestamp.fromSession(session0)
      // Отработать отсутствие таймштампа в сессии.
      if (tstampOpt.isEmpty) {
        if (session0.data contains PersonId.name) {
          // Сессия была только что выставлена в контроллере. Там же и ttl выставлен.
          val session1 = session0 + (Timestamp.name -> LoginTimestamp.currentTstamp.toString)
          result.withSession(session1)
        } else {
          // Не заниматься ковырянием сессии, если юзер не залогинен.
          result
        }

      } else {
        val currTstamp = LoginTimestamp.currentTstamp
        val newTsOpt = tstampOpt
          // Отфильтровать устаревшие timestamp'ы.
          .filter { _.isTimestampValid(currTstamp) }
        val session1 = newTsOpt match {
          case None =>
            // Таймштамп истёк -- стереть из сессии таймштамп и username.
            trace("invokeBlock(): Erasing expired session for person " + session0.get(PersonId.name))
            val filteredKeySet = Keys.onlyLoginIter
              .map(_.name)
              .toSet
            session0.copy(
              data = session0.data
                .filterKeys { k => !(filteredKeySet contains k) }
            )
          // Есть таймштамп, значит пора залить новый (текущий) таймштамп в сессию.
          case Some(ts) =>
            ts.withTstamp(currTstamp)
              .addToSession(session0)
        }
        result withSession session1
      }
    }
  }
}

