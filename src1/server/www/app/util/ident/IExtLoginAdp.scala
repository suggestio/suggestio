package util.ident

import io.suggest.auth.AuthenticationResult
import io.suggest.ext.svc.MExtService
import models.req.MLoginViaReq
import play.api.mvc.{AnyContent, Session}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.02.19 21:43
  * Description: Для возможности переключениями между API'шками, используется система API-адаптеров.
  * Это позволяет абстрагироваться от:
  * - securesocial
  * - pac4j
  * - ...
  */
trait IExtLoginAdp {

  /** Запуск шага аутентификации.
    *
    * @param service Сервис.
    * @param req HTTP-реквест.
    * @return Фьючерс с результатом.
    *         Если совсем неправильный вызов, то будет ошибка внутри Future.
    */
  def authenticate(service: MExtService)(implicit req: MLoginViaReq[AnyContent]): Future[AuthenticationResult]

  def MAX_SESSION_TTL_SECONDS: Long

  def clearSession(s: Session): Session

}
