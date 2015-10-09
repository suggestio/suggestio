package io.suggest.swfs.client.proto.delete

import io.suggest.swfs.client.proto.file.{IFileResponse, IFileResponseStatic}
import play.api.libs.ws.WSResponse

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 10:51
 * Description: Ответ на запрос удаления: тут два варианта, есть файл или не было файла.
 */
object DeleteResponse extends IFileResponseStatic {
  override type T = DeleteResponse
}

/** Интерфейс delete-ответа. */
trait IDeleteResponse extends IFileResponse {

  def isExisted: Boolean

}

/** Экземпляр ответа удачного удаления файла. */
case class DeleteResponse(
  override val occupiedSize: Long
)
  extends IDeleteResponse
{
  override def isExisted: Boolean = true
}


/** Модель ответа, когда удаляемый файл не существует в хранилище. */
object FileNotFoundResponse extends IFileResponseStatic {
  override type T = FileNotFoundResponse
}

/** Ответ, когда удаляемый файл не найден. */
case class FileNotFoundResponse(
  override val occupiedSize: Long
) extends IDeleteResponse {
  override def isExisted    = false
}


case class UnexpectedResponse(raw: WSResponse) extends IDeleteResponse {
  override def isExisted: Boolean = false
  override def occupiedSize: Long = -1L
}
