package io.suggest.sec.util

import com.google.inject.Singleton
import io.suggest.sec.m.msession.Keys
import play.api.mvc.{RequestHeader, Session}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.15 15:17
  * Description: Утиль для работы с данными сессии.
  * Унифицирует самые типовые операции с данными сессии.
  *
  * 2017.feb.28: Логика проверки и поддержания TTL сессии уехала в фильтры.
  * Поэтому данные сессии тут считаются актуальными на момент вызова (TTL не проверяются).
  */
@Singleton
class SessionUtil {

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
  }

}
