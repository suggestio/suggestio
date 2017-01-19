package io.suggest.lk.adv.geo.r.mapf

import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.lk.adv.geo.a.{HandlePopupClose, SetMapCenter}
import io.suggest.lk.adv.geo.m.MMap
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.sjs.leaflet.event.{LocationEvent, PopupEvent}
import japgolly.scalajs.react.{BackendScope, Callback, PropsChildren, ReactComponentB, ReactElement}
import react.leaflet.lmap.{LMapPropsR, LMapR}
import io.suggest.react.ReactCommonUtil.cbFun1TojsCallback

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 17:57
  * Description: React-компонент карты георазмещения.
  * По сути -- обёртка вокруг react-leaflet и diode.
  */
object AdvGeoMapR {

  type Props = ModelProxy[MMap]


  class Backend($: BackendScope[Props, Unit]) {

    def onLocationFound(locEvent: LocationEvent): Callback = {
      val gp = LkAdvGeoFormUtil.latLng2geoPoint( locEvent.latLng )
      $.props >>= { props =>
        props.dispatchCB( SetMapCenter(gp) )
      }
    }

    def onPopupClose(popEvent: PopupEvent): Callback = {
      $.props >>= { props =>
        props.dispatchCB( HandlePopupClose )
      }
    }

    private val onLocationFoundF = cbFun1TojsCallback( onLocationFound )
    private val onPopupCloseF = cbFun1TojsCallback( onPopupClose )

    def render(props: Props, children: PropsChildren) = {
      val v = props()
      // Карта должна рендерится сюда:
      LMapR(
        new LMapPropsR {
          override val center    = LkAdvGeoFormUtil.geoPoint2LatLng( v.props.center )
          override val zoom      = v.props.zoom
          override val className = Css.Lk.Adv.Geo.MAP_CONTAINER
          override val useFlyTo  = true
          override val onLocationFound = {
            if ( v.locationFound.contains(true) ) {
              js.undefined
            } else {
              js.defined( onLocationFoundF )
            }
          }
          override val onPopupClose = js.defined( onPopupCloseF )
        }
      )(children: _*)
    }

  }


  val component = ReactComponentB[Props]("AdvGeoMap")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props)(children: ReactElement*) = component(props, children: _*)

}
