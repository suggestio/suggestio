package io.suggest.swfs.client.play

import io.suggest.swfs.client.proto.delete._
import io.suggest.swfs.client.proto.file.FileOpUnknownResponseException

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 10:34
 * Description: Аддон для swfs play клиента, делающий поддержку удаления.
 */
trait Delete extends ISwfsClientWs {

  override def delete(args: DeleteRequest): Future[Option[DeleteResponse]] = {
    val url = args.toUrl
    val method = "DELETE"

    val startMs = System.currentTimeMillis()
    lazy val logPrefix = s"delete($startMs):"
    LOGGER.trace(s"$logPrefix $method $url\n $args")

    val reqFut = wsClient.url(url).execute(method)

    for (ex <- reqFut.failed) {
      LOGGER.error(s"$logPrefix Failed $method $url\n $args", ex)
    }

    for {
      wsResp <- reqFut
    } yield {

      val s = wsResp.status
      LOGGER.trace(s"$logPrefix $method $url => $s, took ${System.currentTimeMillis - startMs} ms,\n ${wsResp.body}")

      if ( SwfsClientWs.isStatus2xx(s) ) {
        try {
          if (wsResp.body.nonEmpty) {
            wsResp.json
              .validate[DeleteResponse]
              .asOpt
          } else {
            LOGGER.debug(s"$logPrefix Empty 200 resp from swfs. File missing?")
            None
          }
        } catch {
          case ex: Throwable =>
            LOGGER.warn(s"$logPrefix Failed to parse swfs-volume response:\n raw = ${wsResp.body}\n URL = $url\n resp status = ${wsResp.status} ${wsResp.statusText}", ex)
            None
        }

      } else if (s == 404) {
        None
      } else {
        LOGGER.warn(s"Unknown answer received from $method $url => ${wsResp.status} ${wsResp.statusText}:\n ${wsResp.body}")
        throw FileOpUnknownResponseException(method, url, wsResp.status, Some(wsResp.body))
      }
    }
  }

}
