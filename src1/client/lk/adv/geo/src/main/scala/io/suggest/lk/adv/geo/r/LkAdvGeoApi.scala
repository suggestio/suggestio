package io.suggest.lk.adv.geo.r

import io.suggest.adv.geo.{MFormS, MGeoAdvExistPopupResp}
import io.suggest.adv.rcvr.MRcvrPopupResp
import io.suggest.bill.MGetPriceResp
import io.suggest.lk.router.jsRoutes
import io.suggest.maps.m.MMapGjResp
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.common.tags.search.{ITagsApi, TagsApiImplXhr}

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
trait ILkAdvGeoApi extends ITagsApi {

  /** Запрос карты rcvr-маркеров с сервера в виде GeoJSON. */
  def rcvrsMap(adId: String): Future[MMapGjResp]

  /** Запрос с сервера попапа над ресивером. */
  def rcvrPopup(adId: String, nodeId: String): Future[MRcvrPopupResp]

  /** Запрос карты текущий георазмещений с сервера. */
  def existGeoAdvsMap(adId: String): Future[js.Array[GjFeature]]

  /** Запрос содержимого попапа над указанной гео-областью. */
  def existGeoAdvsShapePopup(itemId: Double): Future[MGeoAdvExistPopupResp]

  /** Запросить у сервера рассчёт цены. */
  def getPrice(adId: String, mFormS: MFormS): Future[MGetPriceResp]

  /** Окончательный сабмит формы георазмещения. */
  def formSubmit(adId: String, mFormS: MFormS): Future[String]

  /** Получение инфы по узлу. */
  def rcvrInfoWndBody(nodeId: String, adId: String): Future[String]

}


/** Реализация [[ILkAdvGeoApi]]. */
class LkAdvGeoApiImpl extends ILkAdvGeoApi with TagsApiImplXhr {

  import io.suggest.lk.adv.geo.u.LkAdvGeoRoutes._
  import MRcvrPopupResp.pickler
  import MGeoAdvExistPopupResp.pickler

  /** Функция-генератор роуты поиска тегов на сервере. */
  override protected def _tagsSearchRoute = jsRoutes.controllers.LkAdvGeo.tagsSearch2


  /** Запрос карты rcvr-маркеров с сервера в виде GeoJSON. */
  override def rcvrsMap(adId: String): Future[MMapGjResp] = {
    // Надо запустить запрос на сервер для получения списка узлов.
    val route = jsRoutes.controllers.LkAdvGeo.advRcvrsMap(adId)
    Xhr.requestJson( route )
      .map(MMapGjResp.apply)
  }


  /** Запрос с сервера попапа над ресивером. */
  override def rcvrPopup(adId: String, nodeId: String): Future[MRcvrPopupResp] = {
    val route = jsRoutes.controllers.LkAdvGeo.rcvrMapPopup(
      adId    = adId,
      nodeId  = nodeId
    )
    Xhr.unBooPickleResp[MRcvrPopupResp] {
      Xhr.requestBinary(route)
    }
  }


  override def existGeoAdvsMap(adId: String): Future[js.Array[GjFeature]] = {
    val route = jsRoutes.controllers.LkAdvGeo.existGeoAdvsMap(adId)
    Xhr.requestJson(route)
      .asInstanceOf[Future[js.Array[GjFeature]]]
  }

  override def existGeoAdvsShapePopup(itemId: Double): Future[MGeoAdvExistPopupResp] = {
    val route = jsRoutes.controllers.LkAdvGeo.existGeoAdvsShapePopup(itemId)
    Xhr.unBooPickleResp[MGeoAdvExistPopupResp] {
      Xhr.requestBinary(route)
    }
  }

  /** Запросить у сервера рассчёт цены. */
  override def getPrice(adId: String, mFormS: MFormS): Future[MGetPriceResp] = {
    val route = jsRoutes.controllers.LkAdvGeo.getPriceSubmit(adId)
    val bbuf = PickleUtil.pickle(mFormS)
    Xhr.unBooPickleResp[MGetPriceResp] {
      Xhr.requestBinary(route, bbuf)
    }
  }

  override def formSubmit(adId: String, mFormS: MFormS): Future[String] = {
    val route = jsRoutes.controllers.LkAdvGeo.forAdSubmit(adId)
    val bbuf = PickleUtil.pickle(mFormS)
    val fut = Xhr.successIf200 {
      Xhr.sendBinary(route, bbuf, Xhr.RespTypes.ANY)
    }
    for (xhr <- fut) yield {
      xhr.responseText
    }
  }

  override def rcvrInfoWndBody(nodeId: String, adId: String): Future[String] = {
    val route = jsRoutes.controllers.LkBill2._rcvrInfoWndBody( nodeId, adId )
    Xhr.requestHtml( route )
  }

}
