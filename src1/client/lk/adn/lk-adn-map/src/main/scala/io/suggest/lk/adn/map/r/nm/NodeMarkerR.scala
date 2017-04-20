package io.suggest.lk.adn.map.r.nm

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.geo.MGeoPoint
import io.suggest.lk.adn.map.m.MNodeMarkerS
import io.suggest.maps.r.rad.DraggablePinMarkerR
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import io.suggest.sjs.common.spa.OptFastEq.Plain

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.17 11:49
  * Description: react-компонент маркера узла.
  */
object NodeMarkerR {

  type Props = ModelProxy[MNodeMarkerS]

  protected[this] case class State(
                                    latLngOptC      : ReactConnectProxy[Option[MGeoPoint]]
                                  )


  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): ReactElement = {
      // Маркер центра круга.
      s.latLngOptC { DraggablePinMarkerR.apply }
    }

  }


  val component = ReactComponentB[Props]("NodeMarker")
    .initialState_P { p =>
      State(
        latLngOptC  = p.connect { props =>
          val gp = props.currentCenter
          //val ll = MapsUtil.geoPoint2LatLng( gp )
          Some(gp)
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(nodeMarkerProxy: Props) = component(nodeMarkerProxy)

}
