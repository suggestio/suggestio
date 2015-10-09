package io.suggest.swfs.client.play

import io.suggest.swfs.client.proto.delete._

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 10:34
 * Description: Аддон для swfs play клиента, делающий поддержку удаления.
 */
trait Delete extends ISwfsClientWs {

  override def delete(args: IDeleteRequest)(implicit ec: ExecutionContext): Future[IDeleteResponse] = {
    val url = args.toUrl
    val method = "DELETE"
    for {
      wsResp <- ws.url(url).execute(method)
    } yield {
      val s = wsResp.status
      if ( SwfsClientWs.isStatus2xx(s) ) {
        wsResp.json.validate[DeleteResponse].get
      } else if ( s == 404 ) {
        wsResp.json.validate[FileNotFoundResponse].get
      } else {
        LOGGER.warn(s"Unknown answer received from $method $url => ${wsResp.status} ${wsResp.statusText}:\n ${wsResp.body}")
        UnexpectedResponse(wsResp)
      }
    }
  }

}
