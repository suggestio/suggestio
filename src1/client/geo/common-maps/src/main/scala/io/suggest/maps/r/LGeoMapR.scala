package io.suggest.maps.r

import diode.react.ModelProxy
import io.suggest.maps.m._
import io.suggest.maps.u.MapsUtil
import io.suggest.react.ReactCommonUtil.cbFun1ToJsCb
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.leaflet.event.{Event, LocationEvent, PopupEvent}
import io.suggest.sjs.leaflet.map.LMap
import japgolly.scalajs.react.Callback
import react.leaflet.lmap.LMapPropsR

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 17:57
  * Description: React-компонент карты георазмещения.
  * По сути -- обёртка вокруг react-leaflet и diode.
  */
object LGeoMapR {

  /** Сгенерить пропертисы для LGeoMapR для типичной ситуации отображения карты в ЛК.
    *
    * @param proxy Прокси [[io.suggest.maps.m.MGeoMapPropsR]].
    * @return Инстанс LMapPropsR.
    */
  def lmMapSProxy2lMapProps( proxy: ModelProxy[MGeoMapPropsR] ): LMapPropsR = {
    val v = proxy()

    // Реакция на location found.
    lazy val _onLocationFoundF = {
      def _onLocationFound(locEvent: LocationEvent): Callback = {
        val gp = MapsUtil.latLng2geoPoint( locEvent.latLng )
        proxy.dispatchCB( HandleLocationFound(gp) )
      }
      cbFun1ToJsCb( _onLocationFound )
    }

    // Реакция на закрытие попапа
    val _onPopupCloseF = {
      def _onPopupClose(popEvent: PopupEvent): Callback = {
        proxy.dispatchCB( HandleMapPopupClose )
      }
      cbFun1ToJsCb( _onPopupClose )
    }

    // Реакция на зуммирование карты.
    val _onZoomEndF = {
      def _onZoomEnd(event: Event): Callback = {
        val newZoom = event.target.asInstanceOf[LMap].getZoom()
        proxy.dispatchCB( MapZoomEnd(newZoom) )
      }
      cbFun1ToJsCb( _onZoomEnd )
    }

    // Реакция на перемещение карты.
    val _onMoveEndF = {
      def _onMoveEnd(event: Event): Callback = {
        val newZoom = event.target.asInstanceOf[LMap].getCenter()
        proxy.dispatchCB( MapMoveEnd(newZoom) )
      }
      cbFun1ToJsCb( _onMoveEnd )
    }

    // Карта должна рендерится с такими параметрами:
    new LMapPropsR {
      override val center    = MapsUtil.geoPoint2LatLng( v.center )
      override val zoom      = v.zoom
      // Значение требует markercluster, цифра взята с http://wiki.openstreetmap.org/wiki/Zoom_levels
      override val maxZoom   = 18
      override val useFlyTo  = true
      override val onLocationFound = {
        if ( v.locationFound.contains(true) ) {
          js.undefined
        } else {
          js.defined( _onLocationFoundF )
        }
      }

      //override val trackResize  = JsOptionUtil.opt2undef( v.trackWndResize )
      override val onPopupClose = js.defined( _onPopupCloseF )
      override val onZoomEnd    = js.defined( _onZoomEndF )
      override val onMoveEnd    = js.defined( _onMoveEndF )

      // Пробрасываем extra-пропертисы:
      override val whenReady    = JsOptionUtil.opt2undef( v.whenReady )
      override val className    = JsOptionUtil.opt2undef( v.cssClass )
      override val onDragStart  = JsOptionUtil.opt2undef( v.onDragStart )
      override val onDragEnd    = JsOptionUtil.opt2undef( v.onDragEnd )
    }
  }

}

