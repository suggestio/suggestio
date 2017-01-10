package io.suggest.lk.adv.geo.r.mapf

import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.lk.adv.geo.a.{HandlePopupClose, SetMapCenter}
import io.suggest.lk.adv.geo.m.MMap
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.sjs.leaflet.event.{LocationEvent, PopupEvent}
import japgolly.scalajs.react.{BackendScope, PropsChildren, ReactComponentB, ReactElement}
import react.leaflet.lmap.LMapR

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

    def onLocationFound(locEvent: LocationEvent): Unit = {
      val gp = LkAdvGeoFormUtil.latLng2geoPoint( locEvent.latLng )
      val cb = $.props >>= { props =>
        props.dispatchCB( SetMapCenter(gp) )
      }
      // TODO Надо бы возвращать Callback, но react-leaflet пока это не умеет.
      cb.runNow()
    }

    def onPopupClose(popEvent: PopupEvent): Unit = {
      val cb = $.props >>= { props =>
        props.dispatchCB( HandlePopupClose )
      }
      cb.runNow()
    }

    def render(props: Props, children: PropsChildren) = {
      val v = props()
      // Карта должна рендерится сюда:
      LMapR(
        center    = LkAdvGeoFormUtil.geoPoint2LatLng( v.props.center ),
        zoom      = v.props.zoom,
        className = Css.Lk.Adv.Geo.MAP_CONTAINER,
        useFlyTo  = true,
        onLocationFound = {
          if (v.locationFound.contains(true)) {
            js.undefined
          } else {
            // TODO Нужен Callback тут вместо голой функции?
            onLocationFound _
          }
        },
        onPopupClose = js.defined( onPopupClose )
      )(children: _*)
    }

  }


  val component = ReactComponentB[Props]("AdvGeoMap")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props)(children: ReactElement*) = component(props, children: _*)

}
