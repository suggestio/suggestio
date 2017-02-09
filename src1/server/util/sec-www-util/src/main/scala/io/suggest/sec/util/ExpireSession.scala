package io.suggest.sec.util

import io.suggest.sec.m.msession.Keys._
import io.suggest.sec.m.msession.{Keys, LoginTimestamp}
import io.suggest.util.logs.MacroLogsDyn
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future
import scala.language.higherKinds

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.06.14 17:57
  * Description: Функции для сброса сессии при наступлении таймаута, и соотв.утиль для пролонгации сессии.
  *
  * 2014.feb.06: Из-за добавления в сессию securesocial, csrf-token и т.д. нужно аккуратнее работать с сессией,
  * без использования withNewSession().
  */


/**
 * Трейт, добавляющий в сессию TTL. Добавляется в конце реализации ActionBuilder'а.
 * @tparam R тип реквеста, с которым работаем. Просто форвардится из декларации класса ActionBuilder'а.
 */
trait ExpireSession[R[_]] extends ActionBuilder[R] with MacroLogsDyn {

  abstract override def invokeBlock[A](request: Request[A], block: (R[A]) => Future[Result]): Future[Result] = {
    for (result <- super.invokeBlock(request, block)) yield {
      val session0 = result.session(request)
      val tstampOpt = LoginTimestamp.fromSession(session0)
      // Отработать отсутствие таймштампа в сессии.
      if (tstampOpt.isEmpty) {
        if ( session0.data.contains(PersonId.name) ) {
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
        val session1 = newTsOpt.fold {
          // Таймштамп истёк -- стереть из сессии таймштамп и username.
          LOGGER.trace("invokeBlock(): Erasing expired session for person " + session0.get(PersonId.name))
          val filteredKeySet = Keys.onlyLoginIter
            .map(_.name)
            .toSet
          session0.copy(
            data = session0
              .data
              .filterKeys { k => !filteredKeySet.contains(k) }
          )
          // Есть таймштамп, значит пора залить новый (текущий) таймштамп в сессию.
        }{ ts =>
          ts.withTstamp(currTstamp)
            .addToSession(session0)
        }
        result.withSession( session1 )
      }
    }
  }

}

