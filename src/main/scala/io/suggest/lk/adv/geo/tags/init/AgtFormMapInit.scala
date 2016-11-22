package io.suggest.lk.adv.geo.tags.init

import io.suggest.lk.adv.geo.tags.m.MMapGjResp
import io.suggest.lk.adv.geo.tags.vm.AdIdInp
import io.suggest.lk.router.jsRoutes
import io.suggest.maps.rad.init.RadMapInit
import io.suggest.maps.vm.MapContainer
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.leaflet.map.LMap
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.16 18:36
  * Description: Инициализация карты для формы Adv-geo.
  */
class AgtFormMapInit extends RadMapInit with Log {

  /** Дополнение инициализации карты поддержкой кластеризованных точек узлов-ресиверов. */
  override def initMapOn(rmc: MapContainer): LMap = {
    val lmap = super.initMapOn(rmc)

    // Необходимо отрендерить маркеры узлов-ресиверов, размещенных через lk-adn-map.
    for {
      adIdInp <- AdIdInp.find()
      adId    <- adIdInp.adId
    } {
      val route = jsRoutes.controllers.LkAdvGeo.advRcvrsGeoJson(adId)

      // Надо запустить запрос на сервер для получения списка узлов.
      val gjFut = Xhr.requestJson( route )
        .map(MMapGjResp.apply)

      // Когда придёт ответ, надо начать запихать их всех в L.markercluster:
      gjFut.onComplete {
        case Success(resp) =>
          _handleGeoJson(resp, lmap)
        case Failure(ex) =>
          LOG.error( ErrorMsgs.LK_ADV_GEO_MAP_GJ_REQ_FAIL, ex, msg = route.url )
      }
    }

    lmap
  }


  /** Действо по рендеру полученного GeoJSON на карту размещений. */
  protected def _handleGeoJson(resp: MMapGjResp, lmap: LMap): Unit = {
    ???
  }

}
