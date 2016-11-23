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
import io.suggest.sjs.leaflet.L
import io.suggest.sjs.leaflet.event.{Event, Events}
import io.suggest.sjs.leaflet.marker.MarkerOptions
import io.suggest.sjs.leaflet.marker.cluster._
import io.suggest.sjs.leaflet.marker.icon.IconOptions
import io.suggest.sjs.leaflet.popup.PopupOptions

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
    val mcg = L.markerClusterGroup()

    val po = PopupOptions.empty
    po.closeOnClick = true

    for (gjFeature <- resp.featuresIter) {
      // Собираем параметры отображения маркера.
      val options = MarkerOptions.empty
      options.draggable = false
      options.clickable = true

      // Иконка обязательна, иначе отображать будет нечего. Собрать иконку из присланных сервером данных.
      options.icon  = gjFeature.icon.fold(_pinMarkerIcon()) { iconInfo =>
        val o = IconOptions.empty
        o.iconUrl = iconInfo.url
        // Описываем размеры иконки по данным сервера.
        o.iconSize = L.point(
          x = iconInfo.height,
          y = iconInfo.width
        )
        // Для иконки -- якорь прямо в середине.
        o.iconAnchor = L.point(
          x = iconInfo.height / 2,
          y = iconInfo.width  / 2
        )
        L.icon(o)
      }

      for (title <- gjFeature.title)
        options.title = title

      val marker = L.marker(
        latLng  = gjFeature.pointLatLng,
        options = options
      )

      // center marker drag start. Надо приглушить круг.
      val onClickF = { e: Event =>
        lmap.panTo( marker.getLatLng() )
        marker.bindPopup("TODO", po)
      }
      marker.on3(Events.CLICK, onClickF)

      mcg.addLayer(marker)
    }
    lmap.addLayer( mcg )
  }

}
