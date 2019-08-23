package io.suggest.swfs.client.play

import io.suggest.swfs.client.proto.assign.{AssignResponse, IAssignRequest}
import io.suggest.swfs.client.proto.master.OneMasterRequest
import play.api.http.HttpVerbs
import play.api.libs.ws.WSResponse

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 12:46
 * Description: Кусок реализации play.ws-клиента seaweedfs для поддержки операции /dir/assign.
 */

trait Assign extends ISwfsClientWs with OneMasterRequest { that =>

  override def assign(args: IAssignRequest): Future[AssignResponse] = {

    val startMs = System.currentTimeMillis()
    lazy val logPrefix = s"assign($startMs):"
    LOGGER.trace(s"$logPrefix Starting, args = $args")

    val req = new OneMasterRequestImpl {
      override type Args_t  = IAssignRequest
      override type Res_t   = AssignResponse
      override def _method  = HttpVerbs.POST
      override def _args    = args
      override def requestTimeout = Some( 5.seconds )

      override def _mkUrl(master: String): String = {
        MASTER_PROTO + "://" + master + "/dir/assign" + _args.toQs
      }

      override def _handleResp(url: String, fut: Future[WSResponse]): Future[AssignResponse] = {
        fut.filter { resp =>
          val respBody = resp.body
          // Почему-то при ошибках возвращается 200 Ok с пустым телом ответа.
          val r = _isStatusValid( resp.status ) && !respBody.isEmpty

          def logMsg = s"$logPrefix took ${System.currentTimeMillis() - startMs}ms\n ${_method} $url => ${resp.status} ${resp.statusText}\n $respBody"
          if (r) LOGGER.trace(logMsg)
          else LOGGER.error(logMsg)

          r
        }
        .map { resp =>
          val jsvr = resp.json.validate[Res_t]
          if (jsvr.isError)
            LOGGER.error(s"$logPrefix _handleResp($url): Cannot parse master's reply: $jsvr\n  ${resp.body}")
          jsvr.get
        }
      }
    }
    req.mkOp(MASTERS)
  }

}
