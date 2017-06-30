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

    val streamFut = wsClient.url( url )
      .stream()

    streamFut.onFailure { case ex: Throwable =>
      LOGGER.error(s"$logPrefix Req failed: GET $url\n $args", ex)
    }

    for {
      sr <- streamFut
      //(headers, enumerator)  <-  streamFut
    } yield {

      val respStatus = sr.status
      LOGGER.trace(s"$logPrefix Streaming GET resp, status = $respStatus, took ${System.currentTimeMillis - startMs} ms")

      if ( SwfsClientWs.isStatus2xx(respStatus) ) {
        Some( GetResponse(sr.headers, sr.bodyAsSource) )
      } else if (respStatus == 404) {
        None
      } else {
        LOGGER.warn(s"Unexpected response for GET $url => $respStatus")
        throw FileOpUnknownResponseException("GET", url, respStatus, None)
      }
    }
  }

}
