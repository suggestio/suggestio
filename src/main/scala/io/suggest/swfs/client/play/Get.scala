package io.suggest.swfs.client.play

import io.suggest.swfs.client.proto.file.FileOpUnknownResponseException
import io.suggest.swfs.client.proto.get.{GetResponse, IGetRequest}

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 11:40
 * Description: Чтение файла из хранилища.
 */
trait Get extends ISwfsClientWs {

  override def get(args: IGetRequest): Future[Option[GetResponse]] = {
    val url = args.toUrl

    val startMs = System.currentTimeMillis()
    lazy val logPrefix = s"get($startMs):"
    LOGGER.trace(s"$logPrefix Starting GET $url\n $args")

    val streamFut = wsClient.url( url ).getStream()

    streamFut.onFailure { case ex: Throwable =>
      LOGGER.error(s"$logPrefix Req failed: GET $url\n $args", ex)
    }

    for {
      (headers, enumerator)  <-  streamFut
    } yield {

      val s = headers.status
      LOGGER.trace(s"$logPrefix Streaming GET resp, status = $s, took ${System.currentTimeMillis - startMs} ms")

      if ( SwfsClientWs.isStatus2xx(headers.status) ) {
        Some( GetResponse(headers, enumerator) )
      } else if (headers.status == 404) {
        None
      } else {
        LOGGER.warn(s"Unexpected response for GET $url => ${headers.status}")
        throw FileOpUnknownResponseException("GET", url, headers.status, None)
      }
    }
  }

}
