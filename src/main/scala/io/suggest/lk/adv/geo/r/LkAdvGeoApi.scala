package io.suggest.lk.adv.geo.r

import io.suggest.adv.geo.MRcvrPopupResp
import io.suggest.lk.adv.geo.m.MMapGjResp
import io.suggest.lk.router.jsRoutes
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

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
trait ILkAdvGeoApi {

  /** Запрос карты rcvr-маркеров с сервера в виде GeoJSON. */
  def rcvrsGeoJson(adId: String): Future[MMapGjResp]

  /** Запрос с сервера попапа над ресивером. */
  def rcvrPopup(adId: String, nodeId: String): Future[MRcvrPopupResp]

}


/** Реализация [[ILkAdvGeoApi]]. */
class LkAdvGeoApiImpl extends ILkAdvGeoApi {

  import boopickle.Default._


  /** Запрос карты rcvr-маркеров с сервера в виде GeoJSON. */
  override def rcvrsGeoJson(adId: String): Future[MMapGjResp] = {
    // Надо запустить запрос на сервер для получения списка узлов.
    val route = jsRoutes.controllers.LkAdvGeo.advRcvrsGeoJson(adId)
    Xhr.requestJson( route )
      .map(MMapGjResp.apply)
  }


  /** Запрос с сервера попапа над ресивером. */
  override def rcvrPopup(adId: String, nodeId: String): Future[MRcvrPopupResp] = {
    val route = jsRoutes.controllers.LkAdvGeo.rcvrMapPopup(
      adId    = adId,
      nodeId  = nodeId
    )
    for (bbuf <- Xhr.requestBinary(route)) yield {
      Unpickle[MRcvrPopupResp].fromBytes(bbuf)
    }
  }

}
