package io.suggest.n2.edge.edit.u

import io.suggest.n2.edge.MEdge
import io.suggest.n2.edge.edit.MNodeEdgeIdQs
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model.{HttpReq, HttpReqData}
import io.suggest.routes.routes
import io.suggest.xplay.json.PlayJsonSjsUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.2020 12:37
  * Description: API сервера для редактора эджа.
  */
trait IEdgeEditApi {

  def saveEdge(qs: MNodeEdgeIdQs, edge: MEdge): Future[None.type]

  def deleteEdge(qs: MNodeEdgeIdQs): Future[None.type]

}


class EdgeEditApiHttp extends IEdgeEditApi {

  private def _timeoutMs = Some( 10.seconds.toMillis.toInt )

  override def saveEdge(qs: MNodeEdgeIdQs, edge: MEdge): Future[None.type] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.SysNodeEdges.saveEdge(
          qs = PlayJsonSjsUtil.toNativeJsonObj(
            Json.toJsObject(qs) ) ),
        data = HttpReqData(
          headers = HttpReqData.headersJsonSend,
          body    = Json.toJson( edge ).toString(),
          timeoutMs = _timeoutMs,
        )
      )
    )
      .httpResponseFut
      .successIf20X
      .map( _ => None )
  }

  override def deleteEdge(qs: MNodeEdgeIdQs): Future[None.type] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.SysNodeEdges.deleteEdge(
          qs = PlayJsonSjsUtil.toNativeJsonObj(
            Json.toJsObject(qs ) ) ),
        data = HttpReqData(
          timeoutMs = _timeoutMs,
        )
      )
    )
      .httpResponseFut
      .successIf20X
      .map( _ => None )
  }

}
