package io.suggest.lk.adv.geo.tags.init

import io.suggest.lk.adv.geo.m.MMapGjResp
import io.suggest.lk.adv.geo.tags.fsm.AgtFormFsm
import io.suggest.lk.adv.geo.tags.m.signal.RcvrChanged
import io.suggest.lk.adv.geo.tags.vm.AdIdInp
import io.suggest.lk.adv.geo.tags.vm.popup.rcvr.{NodeCheckBox, Outer}
import io.suggest.lk.router.jsRoutes
import io.suggest.maps.rad.init.RadMapInit
import io.suggest.maps.vm.MapContainer
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.xhr.Xhr
import io.suggest.sjs.leaflet.map.LMap
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.leaflet.L
import io.suggest.sjs.leaflet.event.{Event, Events}
import io.suggest.sjs.leaflet.marker.MarkerOptions
import io.suggest.sjs.leaflet.marker.cluster._
import io.suggest.sjs.leaflet.marker.icon.IconOptions
import io.suggest.sjs.leaflet.popup.PopupOptions
import org.scalajs.dom

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
          _handleGeoJson(resp, lmap, adId)
        case Failure(ex) =>
          LOG.error( ErrorMsgs.LK_ADV_GEO_MAP_GJ_REQ_FAIL, ex, msg = route.url )
      }
    }

    lmap
  }


  /** Действо по рендеру полученного GeoJSON на карту размещений. */
  protected def _handleGeoJson(resp: MMapGjResp, lmap: LMap, adId: String): Unit = {
    val mcg = L.markerClusterGroup()

    val po = PopupOptions.empty
    po.closeOnClick = true

    for (gjFeature <- resp.featuresIter) {
      // Собираем параметры отображения маркера.
      val options = MarkerOptions.empty
      options.draggable = false
      options.clickable = gjFeature.nodeId.isDefined

      // Иконка обязательна, иначе отображать будет нечего. Собрать иконку из присланных сервером данных.
      options.icon  = gjFeature.icon.fold(_pinMarkerIcon()) { iconInfo =>
        val o = IconOptions.empty
        o.iconUrl = iconInfo.url
        // Описываем размеры иконки по данным сервера.
        o.iconSize = L.point(
          y = iconInfo.height,
          x = iconInfo.width
        )
        // Для иконки -- якорь прямо в середине.
        o.iconAnchor = L.point(
          y = iconInfo.height / 2,
          x = iconInfo.width  / 2
        )
        L.icon(o)
      }

      for (title <- gjFeature.title)
        options.title = title

      val marker = L.marker(
        latLng  = gjFeature.pointLatLng,
        options = options
      )

      // Добавить маркер на карту.
      mcg.addLayer(marker)

      // Инициализировать собраный маркер.
      // Узнать nodeId маркера и повесить реакцию на клик.
      for (nodeId <- gjFeature.nodeId) {
        val onClickF = { e: Event =>
          val mLatLng = marker.getLatLng()
          lmap.panTo(mLatLng)
          val route = jsRoutes.controllers.LkAdvGeo.rcvrMapPopup(
            adId    = adId,
            nodeId  = nodeId
          )
          for ( html <- Xhr.requestHtml(route) ) {
            for (content <- Outer.ofNode( VUtil.newDiv(html).firstChild )) {
              // Отобразить попап
              marker
                .bindPopup(content._underlying, po)
                .openPopup()
              // Повесить реакцию в попапе на изменение каких-либо галочек.
              content.addEventListener( "change" ) { event: dom.Event =>
                // Добраться до контейнера полей узла и отправить сигнал в FSM.
                for {
                  ncb   <- NodeCheckBox.ofEventTarget( event.target )
                  ndiv  <- ncb.nodeDiv
                } {
                  AgtFormFsm ! RcvrChanged(ndiv, nodeId)
                }
              }
            }
          }
        }
        marker.on3(Events.CLICK, onClickF)
      }
    }

    lmap.addLayer( mcg )
  }




}
