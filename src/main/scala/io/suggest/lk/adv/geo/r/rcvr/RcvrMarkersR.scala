package io.suggest.lk.adv.geo.r.rcvr

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.lk.adv.geo.a.ReqRcvrPopup
import io.suggest.lk.adv.geo.m.MarkerNodeId
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.sjs.leaflet.marker.{Marker, MarkerEvent}
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import react.leaflet.marker.MarkerClusterGroupR

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 12:50
  * Description: Поддержка простенького react-компонента для кластера ресиверов на карте.
  */
object RcvrMarkersR {

  type Props = ModelProxy[Pot[js.Array[Marker]]]

  protected class Backend($: BackendScope[Props, Unit]) {

    def onMarkerClicked(e: MarkerEvent): Unit = {
      val marker = e.layer
      // TODO Почему-то тут не срабатывает implicit convertion... Приходится явно заворачивать
      val nodeId = MarkerNodeId(marker).nodeId.get
      val latLng = marker.getLatLng()

      val cb = $.props >>= { p =>
        val gp = LkAdvGeoFormUtil.latLng2geoPoint(latLng)
        p.dispatchCB( ReqRcvrPopup(nodeId, gp) )
      }

      // TODO Как сделать callback нормальный для анонимной функции?
      cb.runNow()
    }

    def render(p: Props): ReactElement = {
      p().toOption.fold [ReactElement] (null) { markers =>
        MarkerClusterGroupR(
          markers         = markers,
          onMarkerClick   = onMarkerClicked _
        )()
      }
    }

  }


  val component = ReactComponentB[Props]("RcvrMarkers")
    .stateless
    .renderBackend[Backend]
    .build


  def apply(p: Props) = component(p)

}
