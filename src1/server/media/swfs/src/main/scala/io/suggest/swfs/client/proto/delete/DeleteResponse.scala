package io.suggest.swfs.client.proto.delete

import io.suggest.swfs.client.proto.file.{IFileResponse, IFileResponseStatic}

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
