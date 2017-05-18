package io.suggest.lk.adn.map.r.cur

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.lk.adn.map.m.MCurrentGeoS
import io.suggest.maps.r.ExistAdvGeoShapesR
import io.suggest.sjs.common.geo.json.GjFeature
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import react.leaflet.layer.LayerGroupR

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.17 17:32
  * Description: React-компонент leaflet-элементов, отображающих текущее состояние размещения узла на карте.
  */
object CurrentR {

  type Props = ModelProxy[MCurrentGeoS]

  protected case class State(
                              existingGjC   : ReactConnectProxy[Pot[js.Array[GjFeature]]]
                            )

  protected class Backend($: BackendScope[Props, State]) {

    def render(s: State): ReactElement = {
      LayerGroupR()(

        s.existingGjC { ExistAdvGeoShapesR.apply }

      )
    }

  }


  val component = ReactComponentB[Props]("CurrentGeo")
    .initialState_P { mcgProxy =>
      State(
        existingGjC = mcgProxy.connect(_.existingGj)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(currGeoSProxy: Props) = component(currGeoSProxy)

}
