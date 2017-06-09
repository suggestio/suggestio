package io.suggest.lk.adv.geo.r

import diode.ModelRO
import io.suggest.adv.geo.{MFormS, MGeoAdvExistPopupResp}
import io.suggest.bill.MGetPriceResp
import io.suggest.lk.adv.a.{IRcvrPopupApi, RcvrPopupHttpApiImpl}
import io.suggest.lk.adv.geo.m.MOther
import io.suggest.lk.router.{ILkBill2NodeAdvInfoApi, LkBill2NodeAdvInfoHttpApiImpl, jsRoutes}
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.tags.search.{ITagsApi, TagsHttpApiImpl}
import io.suggest.sjs.common.xhr.Xhr

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
  import io.suggest.lk.adv.geo.u.LkAdvGeoRoutes._

  /** Функция-генератор роуты поиска тегов на сервере. */
  override protected def _tagsSearchRoute = jsRoutes.controllers.LkAdvGeo.tagsSearch2

  override protected def _rcvrPopupRoute(nodeId: String): Route = {
    jsRoutes.controllers.LkAdvGeo.rcvrMapPopup(
      adId    = confRO().adId,
      nodeId  = nodeId
    )
  }

  override def existGeoAdvsMap(): Future[js.Array[GjFeature]] = {
    val route = jsRoutes.controllers.LkAdvGeo.existGeoAdvsMap(
      adId = confRO().adId
    )
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
  override def getPrice(mFormS: MFormS): Future[MGetPriceResp] = {
    val route = jsRoutes.controllers.LkAdvGeo.getPriceSubmit(
      adId = confRO().adId
    )
    val bbuf = PickleUtil.pickle(mFormS)
    Xhr.unBooPickleResp[MGetPriceResp] {
      Xhr.requestBinary(route, bbuf)
    }
  }

  override def formSubmit(mFormS: MFormS): Future[String] = {
    val route = jsRoutes.controllers.LkAdvGeo.forAdSubmit(
      adId = confRO().adId
    )
    val bbuf = PickleUtil.pickle(mFormS)
    val fut = Xhr.successIf200 {
      Xhr.sendBinary(route, bbuf, Xhr.RespTypes.ANY)
    }
    for (xhr <- fut) yield {
      xhr.responseText
    }
  }


  override protected[this] def _nodeAdvInfoRoute(nodeId: String): Route = {
    jsRoutes.controllers.LkBill2.nodeAdvInfo(
      nodeId  = nodeId,
      forAdId = confRO().adId
    )
  }

}
