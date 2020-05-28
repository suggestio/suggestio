package io.suggest.swfs.client.proto.put

import java.io.File

import io.suggest.fio.WriteRequest
import io.suggest.swfs.client.proto.file.IFileRequest

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 13:05
 * Description: Модель аргументов запроса отправки файла в хранилище.
 */
object PutRequest {

  /** Враппер над apply(), где часть аргументов заменена на IWriteRequest. */
  def fromRr(volUrl: String, fid: String, rr: WriteRequest, proto: String = IFileRequest.PROTO_DFLT): PutRequest = {
    apply(
      volUrl        = volUrl,
      fid           = fid,
      file          = rr.file,
      contentType   = rr.contentType,
      origFileName  = rr.origFileName,
      proto         = proto
    )
  }

}


/** Дефолтовая реализация [[IPutRequest]]. */
final case class PutRequest(
                             override val volUrl       : String,
                             override val fid          : String,
                                          file         : File,
                                          contentType  : String,
                                          origFileName : Option[String],
                             override val proto        : String = IFileRequest.PROTO_DFLT
                           )
  extends IFileRequest
{

  def getFileName: String =
    origFileName getOrElse file.getName

}