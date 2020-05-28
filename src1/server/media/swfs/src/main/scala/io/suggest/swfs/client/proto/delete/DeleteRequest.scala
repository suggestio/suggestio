package io.suggest.swfs.client.proto.delete

import io.suggest.swfs.client.proto.file.IFileRequest

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 10:47
 * Description: Реквест удаления файла из хранилища.
 */

final case class DeleteRequest(
                                override val volUrl       : String,
                                override val fid          : String,
                                override val proto        : String = IFileRequest.PROTO_DFLT
                              )
  extends IFileRequest
