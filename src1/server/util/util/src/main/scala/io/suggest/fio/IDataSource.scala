package io.suggest.fio

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.suggest.compress.MCompressAlgo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 23:03
 * Description: Модель данных о результате чтения.
 */

trait IDataSource extends IContentType {

  /** Содержимое файла.
    * Считается, что возвращает один и тот же инстанс Source[].
    */
  def data: Source[ByteString, _]

  /** Размер файла. */
  def sizeB: Long

  /** Если в запросе допускалось использование сжатия, то на выходе ответ может быть сжат. */
  def compression: Option[MCompressAlgo]

}


/*
case class MDataSource(
                        data          : Source[ByteString, _],
                        sizeB         : Long,
                        contentType   : String
                      )
  extends IDataSource
*/
