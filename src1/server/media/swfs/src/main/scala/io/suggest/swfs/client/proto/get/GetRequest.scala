package io.suggest.swfs.client.proto.get

import io.suggest.compress.MCompressAlgo
import io.suggest.swfs.client.proto.file.IFileRequest

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 11:45
 * Description: Модель аргументов запроса на чтение файла из хранилища.
 */
final case class GetRequest(
                             override val volUrl              : String,
                             override val fid                 : String,
                             override val proto               : String                        = IFileRequest.PROTO_DFLT,
                                          acceptCompression   : Iterable[MCompressAlgo]       = Nil
                           )
  extends IFileRequest

