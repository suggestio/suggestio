package io.suggest.swfs.client.play

import io.suggest.swfs.client.proto.file.FileOpUnknownResponseException
import io.suggest.swfs.client.proto.get.{GetResponse, IGetResponse, IGetRequest}

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 11:40
 * Description: Чтение файла из хранилища.
 */
trait Get extends ISwfsClientWs {

  override def get(args: IGetRequest)(implicit ec: ExecutionContext): Future[Option[IGetResponse]] = {
    val url = args.toUrl
    for {
      (headers, enumerator)  <-  ws.url( url ).getStream()
    } yield {
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
