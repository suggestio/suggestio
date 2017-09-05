package io.suggest.maps.r

import diode.react.ModelProxy
import io.suggest.maps.m._
import io.suggest.maps.u.MapsUtil
import io.suggest.react.ReactCommonUtil.cbFun1ToJsCb
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

  type Props = ModelProxy[MMapS]


  /** Сгенерить пропертисы для LGeoMapR для типичной ситуации отображения карты в ЛК.
    *
    * @param dispatcher Прокси MMapS.
    * @return Инстанс LMapPropsR.
    */
  def lmMapSProxy2lMapProps( dispatcher: ModelProxy[MMapS], cssClass: String ): LMapPropsR = {
    lmMapSProxy2lMapProps( dispatcher(), dispatcher, cssClass )
  }

  /** Сгенерить пропертисы для LGeoMapR для типичной ситуации отображения карты в ЛК.
    *
    * @param v Инстанс MMapS.
    * @param dispatcher Прокси для отправки экшенов-событий наверх.
    * @return Инстанс LMapPropsR.
    */
  def lmMapSProxy2lMapProps( v: MMapS, dispatcher: ModelProxy[_], cssClass: String ): LMapPropsR = {

    def _onLocationFound(locEvent: LocationEvent): Callback = {
      val gp = MapsUtil.latLng2geoPoint( locEvent.latLng )
      dispatcher.dispatchCB( HandleLocationFound(gp) )
    }
    lazy val _onLocationFoundF = cbFun1ToJsCb( _onLocationFound )

    def _onPopupClose(popEvent: PopupEvent): Callback = {
      dispatcher.dispatchCB( HandleMapPopupClose )
    }
    val _onPopupCloseF = cbFun1ToJsCb( _onPopupClose )

    def _onZoomEnd(event: Event): Callback = {
      val newZoom = event.target.asInstanceOf[LMap].getZoom()
      dispatcher.dispatchCB( MapZoomEnd(newZoom) )
    }
    val _onZoomEndF = cbFun1ToJsCb( _onZoomEnd )

    // Карта должна рендерится с такими параметрами:
    new LMapPropsR {
      override val center    = MapsUtil.geoPoint2LatLng( v.props.center )
      override val zoom      = v.props.zoom
      // Значение требует markercluster, цифра взята с http://wiki.openstreetmap.org/wiki/Zoom_levels
      override val maxZoom   = 18
      override val className = cssClass
      override val useFlyTo  = true
      override val onLocationFound = {
        if ( v.locationFound.contains(true) ) {
          js.undefined
        } else {
          js.defined( _onLocationFoundF )
        }
      }
      override val onPopupClose = js.defined( _onPopupCloseF )
      override val onZoomEnd = js.defined( _onZoomEndF )
      //override val onMoveEnd = js.defined( _onMoveEndF )    // TODO Бесконечное зацикливание.
    }
  }

}
