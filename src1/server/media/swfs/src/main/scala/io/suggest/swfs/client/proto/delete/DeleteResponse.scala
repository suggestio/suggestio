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

/** Экземпляр ответа удачного удаления файла. */
final case class DeleteResponse(
                                 override val occupiedSize: Long
                               )
  extends IFileResponse
{
  def isExisted: Boolean = true
}
