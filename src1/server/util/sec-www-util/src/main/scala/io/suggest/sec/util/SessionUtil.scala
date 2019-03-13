package io.suggest.sec.util

import javax.inject.Singleton
import io.suggest.session.{LoginTimestamp, MSessionKeys}
import io.suggest.util.logs.MacroLogsImpl
import play.api.mvc.{RequestHeader, Session}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.15 15:17
  * Description: Утиль для работы с данными сессии.
  * Унифицирует самые типовые операции с данными из сессии.
  */
@Singleton
class SessionUtil extends MacroLogsImpl {

  /**
   * Очень часто сюда передаётся реквест, а не сессия. Это укорачивает код.
   * @param request HTTP Реквест.
   * @return Опциональный id юзера.
   */
  def getPersonId(request: RequestHeader): Option[String] = {
    getPersonId(request.session)
  }

  /**
   * Прочитать значение personId из сессии play.
   * Учитывается значение timestamp'а сессиии.
    *
   * @param session Сессия.
   * @return Some(personId) или None.
   */
  def getPersonId(session: Session): Option[String] = {
    session
      .get(MSessionKeys.PersonId.value)
      // Если выставлен timestamp, то проверить валидность защищенного session ttl.
      // НЕЛЬЗЯ удалять отсюда проверку, т.к. в фильтрах (play Filter) и при Action Composition нет возможности
      // нормально перезаписывать сессию реквеста: там lazy val, который перевычисляется заново при каждом последующем Request wrap.
      .filter { personId =>
        val tstampOpt = LoginTimestamp.fromSession(session)
        val result = tstampOpt
          .exists {
            _.isTimestampValid()
          }
        if (!result)
          LOGGER.trace(s"getFromSession(): Session expired for user $personId. tstampRaw = $tstampOpt")
        result
      }
  }

}
