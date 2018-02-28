package io.suggest.swfs.client.proto.get

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.suggest.compress.{MCompressAlgo, MCompressAlgos}
import io.suggest.fio.IDataSource
import play.api.http.HeaderNames

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 11:46
 * Description: Модель ответа на запрос чтения файла из хранилища.
 */
trait IGetResponse extends IDataSource {

  /** Сырые заголовки HTTP-ответа. */
  def headers: Map[String, Seq[String]]

  override def contentType: String = {
    headers
      .get( HeaderNames.CONTENT_TYPE )
      .flatMap(_.headOption)
      .getOrElse("application/octet-stream")
  }

  override def sizeB: Long = {
    headers
      .get( HeaderNames.CONTENT_LENGTH )
      .flatMap(_.headOption)
      .fold(0L)(_.toLong)
  }

  override lazy val compression: Option[MCompressAlgo] = {
    headers
      .get( HeaderNames.CONTENT_ENCODING )
      .iterator
      .flatten
      .map { ce =>
        // Если пришла неизвестная кодировка ответа, то пусть сразу будет ошибка.
        MCompressAlgos.withHttpContentEncoding(ce).get
      }
      .toStream
      .headOption
  }

}


case class GetResponse(
                        override val headers      : Map[String, Seq[String]],
                        override val data         : Source[ByteString, _]
                      )
  extends IGetResponse

