package io.suggest.lk.adv.geo.r

import diode.ModelRO
import io.suggest.adv.geo.{MFormS, MGeoAdvExistPopupResp}
import io.suggest.bill.MGetPriceResp
import io.suggest.lk.adv.a.{IRcvrPopupApi, RcvrPopupHttpApiImpl}
import io.suggest.lk.adv.geo.m.MOther
import io.suggest.pick.PickleUtil
import io.suggest.routes.{ILkBill2NodeAdvInfoApi, LkBill2NodeAdvInfoHttpApiImpl, routes}
import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.tags.search.{ITagsApi, TagsHttpApiImpl}
import io.suggest.sjs.common.xhr.{HttpReq, HttpReqData, HttpRespTypes, Xhr}

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
  def formSubmit(mFormS: MFormS): Future[String]

}


/** Реализация [[ILkAdvGeoApi]]. */
class LkAdvGeoHttpApiImpl( confRO: ModelRO[MOther] )
  extends ILkAdvGeoApi
  with TagsHttpApiImpl
  with RcvrPopupHttpApiImpl
  with LkBill2NodeAdvInfoHttpApiImpl
{

  import MGeoAdvExistPopupResp.pickler

  /** Функция-генератор роуты поиска тегов на сервере. */
  override protected def _tagsSearchRoute = routes.controllers.LkAdvGeo.tagsSearch2

  override protected def _rcvrPopupRoute(nodeId: String): Route = {
    routes.controllers.LkAdvGeo.rcvrMapPopup(
      adId    = confRO().adId,
      nodeId  = nodeId
    )
  }

  override def existGeoAdvsMap(): Future[js.Array[GjFeature]] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkAdvGeo.existGeoAdvsMap(
        adId = confRO().adId
      ),
      data = HttpReqData.justAcceptJson
    )
    Xhr.execute(req)
      .respAuthFut
      .successIf200
      .nativeJsonFut[js.Array[GjFeature]]
  }

  override def existGeoAdvsShapePopup(itemId: Double): Future[MGeoAdvExistPopupResp] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkAdvGeo.existGeoAdvsShapePopup(itemId),
      data  = HttpReqData(
        headers  = HttpReqData.headersBinaryAccept,
        respType = HttpRespTypes.ArrayBuffer
      )
    )
    Xhr.execute( req )
      .respAuthFut
      .successIf200
      .unBooPickle[MGeoAdvExistPopupResp]
  }

  /** Запросить у сервера рассчёт цены. */
  override def getPrice(mFormS: MFormS): Future[MGetPriceResp] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkAdvGeo.getPriceSubmit(
        adId = confRO().adId
      ),
      data = HttpReqData(
        headers   = HttpReqData.headersBinarySendAccept,
        body      = PickleUtil.pickle(mFormS),
        respType  = HttpRespTypes.ArrayBuffer
      )
    )
    Xhr.execute( req )
      .respAuthFut
      .successIf200
      .unBooPickle[MGetPriceResp]
  }

  override def formSubmit(mFormS: MFormS): Future[String] = {
    val req = HttpReq.routed(
      route = routes.controllers.LkAdvGeo.forAdSubmit(
        adId = confRO().adId
      ),
      data = HttpReqData(
        headers   = HttpReqData.headersBinarySend,
        body      = PickleUtil.pickle(mFormS),
        respType  = HttpRespTypes.Default
      )
    )
    Xhr.execute( req )
      .respAuthFut
      .responseTextFut
  }


  override protected[this] def _nodeAdvInfoRoute(nodeId: String): Route = {
    routes.controllers.LkBill2.nodeAdvInfo(
      nodeId  = nodeId,
      forAdId = confRO().adId
    )
  }

}
