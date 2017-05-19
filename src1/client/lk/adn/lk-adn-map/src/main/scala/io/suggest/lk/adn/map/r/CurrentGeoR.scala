package io.suggest.lk.adn.map.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.maps.m.{MExistGeoPopupS, MExistGeoS}
import io.suggest.maps.r.{ExistAdvGeoShapesR, ExistPopupR}
import io.suggest.sjs.common.geo.json.GjFeature
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import react.leaflet.layer.LayerGroupR
import MExistGeoPopupS.MGeoCurPopupSFastEq

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.17 17:32
  * Description: React-компонент leaflet-элементов, отображающих текущее состояние размещения узла на карте.
  */
object CurrentGeoR {

  type Props = ModelProxy[MExistGeoS]

  protected case class State(
                              geoJsonC   : ReactConnectProxy[Pot[js.Array[GjFeature]]],
                              popupC     : ReactConnectProxy[MExistGeoPopupS]
                            )

  protected class Backend($: BackendScope[Props, State]) {

    def render(s: State): ReactElement = {
      LayerGroupR()(

        s.geoJsonC { ExistAdvGeoShapesR.apply },

        // Рендер попапа при клике по шейпу.
        s.popupC { ExistPopupR.apply }

      )
    }

  }


  val component = ReactComponentB[Props]("CurrentGeo")
    .initialState_P { mcgProxy =>
      State(
        geoJsonC = mcgProxy.connect(_.geoJson),
        popupC   = mcgProxy.connect(_.popup)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(currGeoSProxy: Props) = component(currGeoSProxy)

}
