package io.suggest.swfs.client.proto.put

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 13:11
 * Description: Модель ответа сервера на запрос размещения файла.
 */
object PutResponse {

  implicit val FORMAT: Format[PutResponse] = {
    (__ \ "size").format[Long]
      .inmap [PutResponse] (apply, _.size)
  }

}


/** Интерфейс результата модели. */
trait IPutResponse {

  /** Volume-сервер возвращает объем полученного файла. */
  def size: Long

}


case class PutResponse(
  override val size       : Long
)
  extends IPutResponse
