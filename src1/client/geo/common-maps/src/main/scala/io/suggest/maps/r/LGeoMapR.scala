package io.suggest.maps.r

import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.maps.m._
import io.suggest.maps.u.MapsUtil
import io.suggest.react.ReactCommonUtil.cbFun1ToJsCb
import io.suggest.sjs.leaflet.event.{Event, LocationEvent, PopupEvent}
import japgolly.scalajs.react.{BackendScope, Callback, PropsChildren, ReactComponentB, ReactElement}
import react.leaflet.lmap.{LMapPropsR, LMapR}
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.leaflet.map.LMap

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 17:57
  * Description: React-компонент карты георазмещения.
  * По сути -- обёртка вокруг react-leaflet и diode.
  */
object LGeoMapR {

  type Props = ModelProxy[MMapS]


  class Backend($: BackendScope[Props, Unit]) {

    private def _onLocationFound(locEvent: LocationEvent): Callback = {
      val gp = MapsUtil.latLng2geoPoint( locEvent.latLng )
      dispatchOnProxyScopeCB( $, HandleLocationFound(gp) )
    }
    private val _onLocationFoundF = cbFun1ToJsCb( _onLocationFound )


    private def _onPopupClose(popEvent: PopupEvent): Callback = {
      dispatchOnProxyScopeCB( $, HandleMapPopupClose )
    }
    private val _onPopupCloseF = cbFun1ToJsCb( _onPopupClose )


    private def _onZoomEnd(event: Event): Callback = {
      val newZoom = event.target.asInstanceOf[LMap].getZoom()
      dispatchOnProxyScopeCB( $, MapZoomEnd(newZoom) )
    }
    private val _onZoomEndF = cbFun1ToJsCb( _onZoomEnd )


    /*
    private def _onMoveEnd(event: Event): Callback = {
      val newCenterLL = event.target.asInstanceOf[LMap].getCenter()
      dispatchOnProxyScopeCB( $, MapMoveEnd(newCenterLL) )
    }
    private val _onMoveEndF = cbFun1ToJsCb( _onMoveEnd )
    */


    def render(props: Props, children: PropsChildren) = {
      val v = props()
      // Карта должна рендерится сюда:
      LMapR(
        new LMapPropsR {
          override val center    = MapsUtil.geoPoint2LatLng( v.props.center )
          override val zoom      = v.props.zoom

          override val maxZoom   = 16   // TODO Значение требует markercluster.
          override val className = Css.Lk.Maps.MAP_CONTAINER
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
      )(children: _*)
    }

  }


  val component = ReactComponentB[Props]("LGeoMap")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props)(children: ReactElement*) = component(props, children: _*)

}
