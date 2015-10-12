package io.suggest.swfs.client.proto.put

import java.io.File

import io.suggest.fio.IWriteRequest
import io.suggest.swfs.client.proto.file.IFileRequest

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 13:05
 * Description: Модель аргументов запроса отправки файла в хранилище.
 */
trait IPutRequest extends IFileRequest {

  /** Источник данных -- файл. */
  def file            : File

  def contentType     : String

  def origFileName    : Option[String]

  def getFileName: String = {
    origFileName getOrElse file.getName
  }

}


object PutRequest {

  /** Враппер над apply(), где часть аргументов заменена на [[io.suggest.fio.IWriteRequest]]. */
  def fromRr(volUrl: String, fid: String, rr: IWriteRequest, proto: String = IFileRequest.PROTO_DFLT): PutRequest = {
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
case class PutRequest(
  override val volUrl       : String,
  override val fid          : String,
  override val file         : File,
  override val contentType  : String,
  override val origFileName : Option[String],
  override val proto        : String = IFileRequest.PROTO_DFLT
)
  extends IPutRequest
