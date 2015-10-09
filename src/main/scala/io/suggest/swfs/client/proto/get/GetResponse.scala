package io.suggest.swfs.client.proto.get

import play.api.http.HeaderNames
import play.api.libs.iteratee.Enumerator
import play.api.libs.ws.WSResponseHeaders

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 11:46
 * Description: Модель ответа на запрос чтения файла из хранилища.
 */
trait IGetResponse {

  /** Сырые заголовки HTTP-ответа. */
  def headers: WSResponseHeaders

  /** Данные файла. */
  def enumerator: Enumerator[Array[Byte]]

  def contentType: String = {
    headers.headers
      .get( HeaderNames.CONTENT_TYPE )
      .flatMap(_.headOption)
      .getOrElse("application/octet-stream")
  }

  def size: Long = {
    headers.headers
      .get( HeaderNames.CONTENT_LENGTH )
      .flatMap(_.headOption)
      .fold(0L)(_.toLong)
  }

}


case class GetResponse(
  override val headers      : WSResponseHeaders,
  override val enumerator   : Enumerator[Array[Byte]]
)
  extends IGetResponse
