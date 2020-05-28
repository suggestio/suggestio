package io.suggest.swfs.client.proto.put

import io.suggest.swfs.client.proto.file.{IFileResponse, IFileResponseStatic}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 13:11
 * Description: Модель ответа сервера на запрос размещения файла.
 */
object PutResponse extends IFileResponseStatic {

  override type T = PutResponse

}


final case class PutResponse(
                              override val occupiedSize       : Long,
                            )
  extends IFileResponse
