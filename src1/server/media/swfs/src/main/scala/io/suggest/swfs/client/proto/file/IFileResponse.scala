package io.suggest.swfs.client.proto.file

import io.suggest.primo.TypeT
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 10:54
 * Description: Абстрактный ответ на тему файла.
 */
object IFileResponseStatic {

  /** shared-заготовка для JSON format. */
  def FORMAT_CBF = (__ \ "size").format[Long]

}


trait IFileResponseStatic extends TypeT {

  override type T <: IFileResponse

  /** Поддержка JSON. */
  implicit def FORMAT: Format[T] = {
    IFileResponseStatic.FORMAT_CBF
      .inmap [T] (apply, _.occupiedSize)
  }

  def apply(occupiedSize: Long): T

}


/** Интерфейс экземпляров модели. */
trait IFileResponse {

  /** Volume-сервер возвращает объем занятого файлом пространства в хранилище.
    * Например, если файл сжимабелен, то возвращаемый размер будет меньше, чем отправленный файл. */
  def occupiedSize: Long

}
