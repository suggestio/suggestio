package io.suggest.lk.adv.geo.r.rcvr

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.lk.adv.geo.m.ReqRcvrPopup
import io.suggest.maps.u.MapsUtil
import io.suggest.sjs.leaflet.marker.{Marker, MarkerEvent}
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import io.suggest.react.ReactCommonUtil.cbFun1TojsCallback
import io.suggest.react.ReactCommonUtil.Implicits._
import react.leaflet.marker.cluster.{MarkerClusterGroupPropsR, MarkerClusterGroupR}
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.maps.m.MarkerNodeId

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

    def onMarkerClicked(e: MarkerEvent): Callback = {
      val marker = e.layer
      // TODO Почему-то тут не срабатывает implicit convertion... Приходится явно заворачивать
      val nodeId = MarkerNodeId(marker).nodeId.get
      val latLng = marker.getLatLng()

      val gp = MapsUtil.latLng2geoPoint(latLng)
      val msg = ReqRcvrPopup(nodeId, gp)
      dispatchOnProxyScopeCB($, msg)
    }

    private val _onMarkerClickedF = cbFun1TojsCallback( onMarkerClicked )

    def render(p: Props): ReactElement = {
      for (markers1 <- p().toOption) yield {
        MarkerClusterGroupR(
          new MarkerClusterGroupPropsR {
            override val markers      = markers1
            override val markerClick  = _onMarkerClickedF
          }
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
