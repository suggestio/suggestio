package io.suggest.swfs.client.play

import io.suggest.swfs.client.proto.assign.{IAssignRequest, AssignResponse}
import io.suggest.swfs.client.proto.master.OneMasterRequest
import play.api.http.HttpVerbs
import play.api.libs.ws.WSResponse
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 12:46
 * Description: Кусок реализации play.ws-клиента seaweedfs для поддержки операции /dir/assign.
 */

trait Assign extends ISwfsClientWs { that =>

  override def assign(args: IAssignRequest): Future[AssignResponse] = {
    val req = new OneMasterRequest {
      override type Args_t  = IAssignRequest
      override type Res_t   = AssignResponse
      override def _method  = HttpVerbs.POST
      override def _args    = args
      override def _ws      = ws
      override def _ec = ec
      override def LOGGER   = that.LOGGER
      override def _mkUrl(master: String): String = {
        MASTER_PROTO + "://" + master + "/dir/assign" + _args.toQs
      }
      override def _handleResp(url: String, fut: Future[WSResponse]): Future[AssignResponse] = {
        fut.filter { resp =>
          LOGGER.trace(s"${_method} $url replied HTTP ${resp.status} ${resp.statusText}\n ${resp.body}")
          _isStatusValid( resp.status )
        }
        .map { resp =>
          val jsvr = resp.json.validate[Res_t]
          if (jsvr.isError)
            LOGGER.error(s"_handleResp($url): Cannot parse master's reply: $jsvr\n  ${resp.body}")
          jsvr.get
        }
      }
    }
    req.mkOp(MASTERS)
  }

}
