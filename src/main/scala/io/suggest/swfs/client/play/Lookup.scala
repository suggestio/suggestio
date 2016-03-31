package io.suggest.swfs.client.play

import io.suggest.swfs.client.proto.file.FileOpUnknownResponseException
import io.suggest.swfs.client.proto.lookup._
import io.suggest.swfs.client.proto.master.OneMasterRequest
import play.api.http.HttpVerbs
import play.api.libs.ws.WSResponse

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 14:51
 * Description: Поддержка Lookup в swfs-клиенте.
 */
trait Lookup extends ISwfsClientWs { that =>

  override def lookup(args: ILookupRequest): Future[Either[LookupError, LookupResponse]] = {
    // Готовим логгирование.
    val startMs = System.currentTimeMillis()
    lazy val logPrefix = s"lookup($startMs):"
    LOGGER.trace(s"$logPrefix Starting, args = $args")

    // Собираем и запускаем запрос...
    val req = new OneMasterRequest {
      override type Args_t          = ILookupRequest
      override type Res_t           = Either[LookupError, LookupResponse]
      override def _args            = args
      override def _method          = HttpVerbs.GET
      override def _ws              = ws
      override def _ec              = ec
      override def LOGGER           = that.LOGGER
      override def _mkUrl(master: String): String = {
        MASTER_PROTO + "://" + master + "/dir/lookup" + args.toQs
      }
      override def _handleResp(url: String, fut: Future[WSResponse]): Future[Res_t] = {
        for (wsResp <- fut) yield {
          val s = wsResp.status
          LOGGER.trace(s"$logPrefix Received answer from $url: HTTP $s, took = ${System.currentTimeMillis() - startMs} ms.\n ${wsResp.body}")

          if ( SwfsClientWs.isStatus2xx(s) ) {
            Right( wsResp.json.as[LookupResponse] )
          } else if (s == 404) {
            Left( wsResp.json.as[LookupError] )
          } else {
            LOGGER.error(s"$logPrefix Unexpected answer ($s) from $url: $wsResp")
            throw FileOpUnknownResponseException(_method, url, s, Some(wsResp.body))
          }
        }
      }
    }
    req.mkOp(MASTERS)
  }

}
