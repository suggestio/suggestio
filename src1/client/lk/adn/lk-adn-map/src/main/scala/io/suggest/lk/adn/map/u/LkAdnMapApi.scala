package io.suggest.lk.adn.map.u

import io.suggest.adn.mapf.MLamForm
import io.suggest.adv.geo.MGeoAdvExistPopupResp
import io.suggest.bill.MGetPriceResp
import io.suggest.geo.json.GjFeature
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.routes.{ILkBill2NodeAdvInfoApi, LkBill2NodeAdvInfoHttpApiImpl, routes}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 16:22
  * Description: Серверная APIшка до контроллера LkAdnMap.
  */
trait ILkAdnMapApi
  extends ILkBill2NodeAdvInfoApi
{

  /** Получение ценника. */
  def getPriceSubmit(nodeId: String, mForm: MLamForm): Future[MGetPriceResp]

  /** Сабмит формы на сервер. */
  def forNodeSubmit(nodeId: String, mForm: MLamForm): Future[String]

  /** Получение GeoJSON'а текущих размещений узла. */
  def currentNodeGeoGj(nodeId: String): Future[js.Array[GjFeature]]

  def currentGeoItemPopup(itemId: Double): Future[MGeoAdvExistPopupResp]

}


/** Реализация [[ILkAdnMapApi]] поверх обычного HTTP. */
class LkAdnMapApiHttpImpl
  extends ILkAdnMapApi
  with LkBill2NodeAdvInfoHttpApiImpl
{

  import LkAdnMapControllers._


  override def getPriceSubmit(nodeId: String, mForm: MLamForm): Future[MGetPriceResp] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkAdnMap.getPriceSubmit( nodeId ),
      data  = HttpReqData(
        headers   = HttpReqData.headersJsonSendAccept,
        body      = Json.toJson( mForm ).toString(),
      )
    )
    HttpClient.execute( req )
      .respAuthFut
      .successIf200
      .unJson[MGetPriceResp]
  }


  override def forNodeSubmit(nodeId: String, mForm: MLamForm): Future[String] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkAdnMap.forNodeSubmit( nodeId ),
      data  = HttpReqData(
        headers   = HttpReqData.headersJsonSend +
          (HttpConst.Headers.ACCEPT -> MimeConst.TEXT_PLAIN),
        body      = Json.toJson( mForm ).toString(),
        respType  = HttpRespTypes.Default
      )
    )
    HttpClient.execute( req )
      .respAuthFut
      .successIf200
      .responseTextFut
  }


  override def currentNodeGeoGj(nodeId: String): Future[js.Array[GjFeature]] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkAdnMap.currentNodeGeoGj( nodeId ),
      data  = HttpReqData.justAcceptJson
    )
    HttpClient.execute( req )
      .respAuthFut
      .successIf200
      .nativeJsonFut[js.Array[GjFeature]]
  }


  override def currentGeoItemPopup(itemId: Double): Future[MGeoAdvExistPopupResp] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkAdnMap.currentGeoItemPopup(itemId),
      data  = HttpReqData(
        headers   = HttpReqData.headersBinaryAccept,
        respType  = HttpRespTypes.ArrayBuffer
      )
    )
    HttpClient.execute(req)
      .respAuthFut
      .successIf200
      .unJson[MGeoAdvExistPopupResp]
  }

}
