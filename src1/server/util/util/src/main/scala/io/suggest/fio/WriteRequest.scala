package io.suggest.fio

import java.io.File

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 22:36
 * Description: Модель аргументов запроса сохранения файла в абстрактное хранилище.
 */
trait IWriteRequest extends IContentType {

  /** Файл в файловой системе. */
  def file: File

  /** Оригинальное имя файла, если есть. */
  def origFileName: Option[String]

}


case class WriteRequest(
  override val contentType  : String,
  override val file         : File,
  override val origFileName : Option[String] = None
)
  extends IWriteRequest
