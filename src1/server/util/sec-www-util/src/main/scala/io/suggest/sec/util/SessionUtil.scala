package io.suggest.sec.util

import io.suggest.session.MSessionKeys
import io.suggest.util.logs.MacroLogsImpl
import play.api.mvc.{RequestHeader, Session}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.15 15:17
  * Description: Утиль для работы с данными сессии.
  * Унифицирует самые типовые операции с данными из сессии.
  */
final class SessionUtil extends MacroLogsImpl {

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
    // До между play ~2.4 и 2.7.0 тут был код фильтрации сессии юзера по TTL.
    // Сейчас проверка снова переехала в фильтр ExpireSession, где она по логике и должна жить.
  }

}
