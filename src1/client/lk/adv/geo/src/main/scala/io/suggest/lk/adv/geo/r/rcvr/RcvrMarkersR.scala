package io.suggest.lk.adv.geo.r.rcvr

import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.lk.adv.geo.a.ReqRcvrPopup
import io.suggest.lk.adv.geo.m.MarkerNodeId
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.sjs.leaflet.marker.{Marker, MarkerEvent}
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement}
import io.suggest.react.ReactCommonUtil.callBackFun2jsCallback
import io.suggest.react.ReactCommonUtil.Implicits._
import react.leaflet.marker.cluster.{MarkerClusterGroupPropsR, MarkerClusterGroupR}

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

      $.props >>= { p =>
        val gp = LkAdvGeoFormUtil.latLng2geoPoint(latLng)
        p.dispatchCB( ReqRcvrPopup(nodeId, gp) )
      }
    }

    private val _onMarkerClickedF = callBackFun2jsCallback( onMarkerClicked )

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
