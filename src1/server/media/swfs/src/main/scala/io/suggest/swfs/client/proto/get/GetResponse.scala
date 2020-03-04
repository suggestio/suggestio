package io.suggest.swfs.client.proto.get

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.suggest.compress.{MCompressAlgo, MCompressAlgos}
import io.suggest.fio.IDataSource
import io.suggest.pick.MimeConst
import play.api.http.HeaderNames

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 11:46
 * Description: Модель ответа на запрос чтения файла из хранилища.
 */
case class GetResponse(
                        headers                   : Map[String, collection.Seq[String]],
                        override val data         : Source[ByteString, _],
                      )
  extends IDataSource
{

  override lazy val contentType: String = {
    (for {
      hdrVs <- headers.get( HeaderNames.CONTENT_TYPE ).iterator
      hdr <- hdrVs
      if hdr.nonEmpty
    } yield {
      hdr
    })
      .nextOption()
      .getOrElse( MimeConst.APPLICATION_OCTET_STREAM )
  }

  override lazy val sizeB: Long = {
    (for {
      hdrVs <- headers.get( HeaderNames.CONTENT_LENGTH ).iterator
      hdr   <- hdrVs
      if hdr.nonEmpty
    } yield {
      hdr.toLong
    })
      .nextOption()
      .getOrElse( 0L )
  }

  override lazy val compression: Option[MCompressAlgo] = {
    (for {
      hdrVs <- headers.get( HeaderNames.CONTENT_ENCODING ).iterator
      hdr   <- hdrVs
      if hdr.nonEmpty
    } yield {
      // Если пришла неизвестная кодировка ответа, то пусть сразу будет ошибка.
      MCompressAlgos
        .withHttpContentEncoding(hdr)
        .get
    })
      .nextOption()
  }

}
