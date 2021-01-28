package io.suggest.lk.adn.map.r

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProps, ReactConnectProxy}
import io.suggest.maps.m.{MExistGeoPopupS, MExistGeoS}
import io.suggest.maps.r.{ExistAdvGeoShapesR, ExistPopupR}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import MExistGeoPopupS.MGeoCurPopupSFastEq
import io.suggest.geo.json.GjFeature
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.Implicits._
import org.js.react.leaflet.LayerGroup

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.17 17:32
  * Description: React-компонент leaflet-элементов, отображающих текущее состояние размещения узла на карте.
  */
final class CurrentGeoR {

  type Props_t = MExistGeoS
  type Props = ModelProxy[Props_t]

  protected case class State(
                              geoJsonC   : ReactConnectProxy[Pot[js.Array[GjFeature]]],
                              popupC     : ReactConnectProxy[MExistGeoPopupS]
                            )

  protected class Backend($: BackendScope[Props, State]) {

    def render(s: State): VdomElement = {
      LayerGroup()(

        s.geoJsonC { ExistAdvGeoShapesR.component.apply },

        // Рендер попапа при клике по шейпу.
        s.popupC { ExistPopupR.component.apply }

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mcgProxy =>
      State(
        geoJsonC = mcgProxy.connect(_.geoJson),
        popupC   = mcgProxy.connect(_.popup)
      )
    }
    .renderBackend[Backend]
    .build

}
