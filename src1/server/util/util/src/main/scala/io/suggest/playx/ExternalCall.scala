package io.suggest.playx

import play.api.mvc.{Call, RequestHeader}
import play.mvc.Http.Request

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.01.17 22:10
  */

/**
 * Экземпляр хранит вызов к внешнему серверу. Кроме как для индикации этого факта, класс ни для чего
 * больше не используется.
 * @param url Ссылка для вызова.
 * @param method - Обычно "GET", который по умолчанию и есть.
 */
class ExternalCall(
  url     : String,
  method  : String = "GET"
)
  extends Call(method = method, url = url)
{

  override def absoluteURL(secure: Boolean)(implicit request: RequestHeader): String = url
  override def absoluteURL(request: Request): String = url
  override def absoluteURL(request: Request, secure: Boolean): String = url
  override def absoluteURL(secure: Boolean, host: String): String = url

}


