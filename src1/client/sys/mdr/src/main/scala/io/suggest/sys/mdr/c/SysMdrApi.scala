package io.suggest.sys.mdr.c

import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.routes.routes
import io.suggest.sys.mdr.{MMdrNextResp, MMdrResolution, MdrSearchArgs}
import io.suggest.xplay.json.PlayJsonSjsUtil
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.10.18 22:34
  * Description: Серверное API для SysMdr-контроллера.
  */
trait ISysMdrApi {

  /** Запрос на сервер за данными для модерации. */
  def nextMdrInfo(args: MdrSearchArgs): Future[MMdrNextResp]

  /** Запрос на сервер с резолюцией модератора. */
  def doMdr(mdrRes: MMdrResolution): Future[_]

  /** Запрос на ремонт узла. */
  def fixNode(nodeId: String): Future[_]

}


class SysMdrApiXhrImpl extends ISysMdrApi {

  override def nextMdrInfo(args: MdrSearchArgs): Future[MMdrNextResp] = {
    val req = HttpReq.routed(
      route = routes.controllers.SysMdr.nextMdrInfo(
        args = PlayJsonSjsUtil.toNativeJsonObj( Json.toJsObject(args) )
      ),
      data = HttpReqData.justAcceptJson
    )
    HttpClient.execute(req)
      .respAuthFut
      .successIf200
      .unJson[MMdrNextResp]
  }


  override def doMdr(mdrRes: MMdrResolution): Future[_] = {
    val req = HttpReq.routed(
      route = routes.controllers.SysMdr.doMdr(
        mdrRes = PlayJsonSjsUtil.toNativeJsonObj( Json.toJsObject(mdrRes) )
      ),
      data = HttpReqData.justAcceptJson
    )
    HttpClient.execute(req)
      .respAuthFut
      .successIfStatus( HttpConst.Status.NO_CONTENT )
      // TODO Десериализовать/обработать ответ.
  }


  override def fixNode(nodeId: String): Future[_] = {
    val req = HttpReq.routed(
      route = routes.controllers.SysMdr.fixNode(
        nodeId = nodeId
      )
    )
    HttpClient.execute( req )
      .respAuthFut
      .successIfStatus( HttpConst.Status.NO_CONTENT )
  }

}
