package io.suggest.fio

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.suggest.compress.MCompressAlgo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 23:03
 * Description: Интерфейс доступа к данным на чтение.
 */

trait IDataSource {

  def contentType: String

  /** Содержимое файла.
    * Считается, что возвращает один и тот же инстанс Source[].
    */
  def data: Source[ByteString, _]

  /** Размер файла. */
  def sizeB: Long

  /** Если в запросе допускалось использование сжатия, то на выходе ответ может быть сжат. */
  def compression: Option[MCompressAlgo]

  /** Если ответ содержит запрошенные ranges, то тут значение HTTP Content-Range.
    * Для range-ответов надо возвращать клиенту 206 Partial Content и всё такое.
    */
  def httpContentRange: Option[String] = None

}
