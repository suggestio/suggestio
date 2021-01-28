package io.suggest.lk.adn.map.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.geo.MGeoPoint
import io.suggest.lk.adn.map.m.MLamRcvrs
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.r.RcvrMarkersR
import io.suggest.maps.u.{MapIcons, MapsUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.spa.OptFastEq.Plain
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import japgolly.scalajs.react.{BackendScope, PropsChildren, ScalaComponent}
import org.js.react.leaflet.LayerGroup

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.17 19:06
  * Description: React-компонент для отображения на карте разных других ресиверов.
  * Рендерится внутри компонента Leaflet-карты.
  */
final class LamRcvrsR {

  type Props = ModelProxy[MLamRcvrs]

  protected case class State(
                              nodesRespPotC     : ReactConnectProxy[Pot[MGeoNodesResp]],
                              popupRespPotC     : ReactConnectProxy[Option[MGeoPoint]]
                            )

  class Backend($: BackendScope[Props, State]) {

    def render(s: State, children: PropsChildren): VdomElement = {
      LayerGroup()(

        // Рендер гео.карты узлов-ресиверов. Сейчас она такая же, как и карта в lk-adv-geo:
        s.nodesRespPotC {
          RcvrMarkersR.component(_)(children)
        },

        // Рендерить крутилку на карте, пока с сервера происходит подгрузка данных для попапа:
        s.popupRespPotC { mgpOptProxy =>
          mgpOptProxy().whenDefinedEl { mgp =>
            val latLng = MapsUtil.geoPoint2LatLng(mgp)
            MapIcons.preloaderLMarker(latLng)
          }
        }

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { p =>
      State(
        nodesRespPotC = p.connect(_.nodesResp),
        popupRespPotC = p.connect { m =>
          for {
            popupState <- m.popupState
            if m.popupResp.isPending
          } yield {
            popupState.latLng
          }
        }
      )
    }
    .renderBackendWithChildren[Backend]
    .build

}
