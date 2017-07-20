package io.suggest.sc.search.v

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.maps.m.MMapS
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.r.{LGeoMapR, RcvrMarkersR, ReactLeafletUtil}
import io.suggest.sc.search.m.MScSearch
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import react.leaflet.control.LocateControlR
import react.leaflet.lmap.LMapR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 14:24
  * Description: Компонент поисковой панели (живёт справа).
  */
object SearchR {

  import MMapS.MMapSFastEq

  type Props = ModelProxy[MScSearch]


  protected[this] case class State(
                                    mmapC               : ReactConnectProxy[MMapS],
                                    rcvrsGeoC           : ReactConnectProxy[Pot[MGeoNodesResp]]
                                  )

  class Backend( $: BackendScope[Props, State] ) {

    def render(s: State): VdomElement = {
      s.mmapC { mmapS =>
        val lMapProps = LGeoMapR.lmMapSProxy2lMapProps( mmapS )

        LMapR(lMapProps)(

            // Рендерим основную плитку карты.
            ReactLeafletUtil.Tiles.OsmDefault,

            // Плагин для геолокации текущего юзера.
            LocateControlR(),

            // Рендер шейпов и маркеров текущий узлов.
            s.rcvrsGeoC( RcvrMarkersR(_)() )

        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("Search")
    .initialStateFromProps { propsProxy =>
      State(
        mmapC = propsProxy.connect(_.mapState),
        rcvrsGeoC = propsProxy.connect(_.rcvrsGeo)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scSearchProxy: Props) = component( scSearchProxy )

}
