package io.suggest.lk.adv.geo.r

import diode.ModelRO
import io.suggest.adv.geo.{MFormS, MGeoAdvExistPopupResp}
import io.suggest.bill.MGetPriceResp
import io.suggest.geo.json.GjFeature
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.lk.adv.a.{IRcvrPopupApi, RcvrPopupHttpApiImpl}
import io.suggest.lk.adv.geo.m.MOther
import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.routes.{ILkBill2NodeAdvInfoApi, LkBill2NodeAdvInfoHttpApiImpl, PlayRoute, routes}
import io.suggest.tags.{ITagsApi, TagsHttpApiImpl}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 13:11
  * Description: API наподобии RPC для взаимодействия js-формы с удалённым контроллером LkAdvGeo.
  * Сделано по мотивам autowire RPC, но без auto, т.к. слишком много гемора там получается с ACL и остальным.
  *
  * Интерфейс отделён от реализации, чтобы его можно было в будущем быстро вынести за пределы этого под-проекта.
  *
  * @see [[https://geirsson.com/post/2015/10/autowire-acl/]] - почему не autowire.
  */
trait ILkAdvGeoApi
  extends ITagsApi
  with IRcvrPopupApi
  with ILkBill2NodeAdvInfoApi
{

  /** Запрос карты текущий георазмещений с сервера. */
  def existGeoAdvsMap(): Future[js.Array[GjFeature]]

  /** Запрос содержимого попапа над указанной гео-областью. */
  def existGeoAdvsShapePopup(itemId: Double): Future[MGeoAdvExistPopupResp]

  /** Запросить у сервера рассчёт цены. */
  def getPrice(mFormS: MFormS): Future[MGetPriceResp]

  /** Окончательный сабмит формы георазмещения. */
  def forAdSubmit(mFormS: MFormS): Future[String]

}


/** Реализация [[ILkAdvGeoApi]]. */
class LkAdvGeoHttpApiImpl( confRO: ModelRO[MOther] )
  extends ILkAdvGeoApi
  with TagsHttpApiImpl
  with RcvrPopupHttpApiImpl
  with LkBill2NodeAdvInfoHttpApiImpl
{

  /** Функция-генератор роуты поиска тегов на сервере. */
  override protected def _tagsSearchRoute = routes.controllers.LkAdvGeo.tagsSearch2

  override protected def _rcvrPopupRoute(nodeId: String): PlayRoute = {
    routes.controllers.LkAdvGeo.rcvrMapPopup(
      adId    = confRO().adId,
      nodeId  = nodeId
    )
  }

  override def existGeoAdvsMap(): Future[js.Array[GjFeature]] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.LkAdvGeo.existGeoAdvsMap(
          adId = confRO().adId
        ),
        data = HttpReqData.justAcceptJson
      )
    )
      .respAuthFut
      .successIf200
      .nativeJsonFut[js.Array[GjFeature]]
  }

  override def existGeoAdvsShapePopup(itemId: Double): Future[MGeoAdvExistPopupResp] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.LkAdvGeo.existGeoAdvsShapePopup(itemId),
        data  = HttpReqData.justAcceptJson,
      )
    )
      .respAuthFut
      .successIf200
      .unJson[MGeoAdvExistPopupResp]
  }

  /** Запросить у сервера рассчёт цены. */
  override def getPrice(mFormS: MFormS): Future[MGetPriceResp] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.LkAdvGeo.getPriceSubmit(
          adId = confRO().adId
        ),
        data = HttpReqData(
          headers   = HttpReqData.headersJsonSendAccept,
          body      = Json.toJson( mFormS ).toString,
        )
      )
    )
      .respAuthFut
      .successIf200
      .unJson[MGetPriceResp]
  }

  override def forAdSubmit(mFormS: MFormS): Future[String] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.LkAdvGeo.forAdSubmit(
          adId = confRO().adId
        ),
        data = HttpReqData(
          headers   = HttpReqData.headersJsonSend +
            (HttpConst.Headers.ACCEPT -> MimeConst.TEXT_PLAIN),
          body      = Json.toJson( mFormS ).toString(),
          respType  = HttpRespTypes.Default
        )
      )
    )
      .respAuthFut
      .successIf200
      .responseTextFut
  }


  override protected[this] def _nodeAdvInfoRoute(nodeId: String): PlayRoute = {
    routes.controllers.LkBill2.nodeAdvInfo(
      nodeId  = nodeId,
      forAdId = confRO().adId
    )
  }

}
