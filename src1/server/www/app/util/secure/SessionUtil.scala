package util.secure

import com.google.inject.Singleton
import io.suggest.util.logs.MacroLogsImpl
import models.msession.{Keys, LoginTimestamp}
import play.api.mvc.{RequestHeader, Session}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.12.15 15:17
 * Description: Утиль для работы с сессией.
 * Появилась при упорядочивании разбросанных по всему проекту
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
   * @param session Сессия.
   * @return Some(personId) или None.
   */
  def getPersonId(session: Session): Option[String] = {
    session
      .get(Keys.PersonId.name)
      // Если выставлен timestamp, то проверить валидность защищенного session ttl.
      .filter { personId =>
        val tstampOpt = LoginTimestamp.fromSession(session)
        val result = tstampOpt
          .exists { _.isTimestampValid() }
        if (!result)
          LOGGER.trace(s"getFromSession(): Session expired for user $personId. tstampRaw = $tstampOpt")
        result
      }
  }

}
