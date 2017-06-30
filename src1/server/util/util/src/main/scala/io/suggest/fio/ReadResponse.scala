package io.suggest.fio

import akka.stream.scaladsl.Source
import akka.util.ByteString

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 23:03
 * Description: Модель данных о результате чтения.
 */

trait IReadResponse extends IContentType {

  /** Содержимое файла. */
  def data: Source[ByteString, _]

  /** Размер файла. */
  def sizeB: Long

}


/*
case class ReadResponse(
  override val contentType  : String,
  override val data         : Source[ByteString, _],
  override val sizeB        : Long
)
  extends IReadResponse
*/
