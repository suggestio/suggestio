package io.suggest.swfs.client.proto.put

import java.io.File

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 13:05
 * Description: Модель аргументов запроса отправки файла в хранилище.
 */
trait IPutRequest {

  /**
   * Используемый протокол.
   * @return Например "http".
   */
  def proto           : String

  /**
   * "Ссылка" на раздел (в терминологии seaweedfs).
   * @return Например "localhost:8080".
   */
  def volUrl          : String

  /** fid, т.е. раздел и id файла. */
  def fid             : String

  /** Источник данных -- файл. */
  def file            : File

  def contentType     : String

  def fileName        : Option[String]

  def getFileName: String = {
    fileName getOrElse file.getName
  }

  /** Сборка URL запроса. */
  def toUrl: String = {
    proto + "://" + volUrl + "/" + fid
  }

}


/** Дефолтовая реализация [[IPutRequest]]. */
case class PutRequest(
  override val volUrl       : String,
  override val fid          : String,
  override val file         : File,
  override val contentType  : String,
  override val fileName     : Option[String],
  override val proto        : String = "http"
)
  extends IPutRequest
