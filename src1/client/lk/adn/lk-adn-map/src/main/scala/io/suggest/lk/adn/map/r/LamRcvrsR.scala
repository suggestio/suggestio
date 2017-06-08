package io.suggest.lk.adn.map.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adn.map.m.MLamRcvrs
import io.suggest.lk.adv.m.IRcvrPopupProps
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.r.RcvrMarkersR
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import react.leaflet.layer.LayerGroupR
import IRcvrPopupProps.IRcvrPopupPropsFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.17 19:06
  * Description: React-компонент для отображения на карте разных других ресиверов.
  * Рендерится внутри компонента Leaflet-карты.
  */
object LamRcvrsR {

  type Props = ModelProxy[MLamRcvrs]

  protected case class State(
                              nodesRespPotC     : ReactConnectProxy[Pot[MGeoNodesResp]],
                              irppC             : ReactConnectProxy[IRcvrPopupProps]
                            )

  class Backend($: BackendScope[Props, State]) {

    def render(s: State): ReactElement = {
      LayerGroupR()(

        // Рендер гео.карты узлов-ресиверов. Сейчас она такая же, как и карта в lk-adv-geo:
        s.nodesRespPotC { RcvrMarkersR.apply },

        // Рендер попапа над ресивером.
        s.irppC { LamRcvrPopupR.apply }

      )
    }

  }


  val component = ReactComponentB[Props]("LamRcvrs")
    .initialState_P { p =>
      State(
        nodesRespPotC = p.connect(_.nodesResp),
        irppC         = p.connect(identity)
      )
    }
    .renderBackend[Backend]
    .build


  def apply(lamRcvrsProxy: Props) = component( lamRcvrsProxy )

}
