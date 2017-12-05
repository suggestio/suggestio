package io.suggest.sc.search.v

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.maps.m.{MGeoMapPropsR, MMapS, MapDragEnd}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.r.{LGeoMapR, RcvrMarkersR, ReactLeafletUtil}
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.styl.GetScCssF
import io.suggest.sjs.leaflet.event.DragEndEvent
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import react.leaflet.control.LocateControlR
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.search.m.MMapInitState
import react.leaflet.lmap.LMapR

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.11.17 21:52
  * Description: Компонент географической карты для панели поиска.
  */
class SearchMapR(
                  getScCssF  : GetScCssF
                ) {

  import MGeoMapPropsR.MGeoMapPropsRFastEq
  import MMapS.MMapSFastEq4Map


  protected[this] case class State(
                                    mmapC       : ReactConnectProxy[MMapS],
                                    rcvrsGeoC   : ReactConnectProxy[Pot[MGeoNodesResp]]
                                  )


  type Props = ModelProxy[MMapInitState]

  class Backend($: BackendScope[Props, State]) {

    /*
    private def _onMapDragStart(e: Event): Callback = {
      dispatchOnProxyScopeCB( $, MapDragStart )
    }
    private val _onMapDragStartOptF = Some( ReactCommonUtil.cbFun1ToJsCb( _onMapDragStart ) )
    */


    private def _onMapDragEnd(e: DragEndEvent): Callback = {
      dispatchOnProxyScopeCB( $, MapDragEnd(distancePx = e.distance) )
    }
    private val _onMapDragEndOptF = Some( ReactCommonUtil.cbFun1ToJsCb( _onMapDragEnd ) )


    def render(mapInitProxy: Props, s: State): VdomElement = {
      val mapCSS = getScCssF().Search.Tabs.MapTab
      val mapInit = mapInitProxy.value

      <.div(
        mapCSS.outer,

        <.div(
          mapCSS.wrapper,

          <.div(
            mapCSS.inner,

            if (!mapInit.ready) {
              // Инициализация карты пока не запущена даже.
              // TODO Выводить "Пжлст, подождите..."?
              EmptyVdom

            } else {
              s.mmapC { mmapProxy =>
                mmapProxy.wrap { mmap =>
                  MGeoMapPropsR(
                    center        = mmap.center,
                    zoom          = mmap.zoom,
                    locationFound = mmap.locationFound,
                    cssClass      = Some( mapCSS.geomap.htmlClass ),
                    //onDragStart   = _onMapDragStartOptF,
                    onDragEnd     = _onMapDragEndOptF
                  )
                } { geoMapPropsProxy =>
                  LMapR(
                    LGeoMapR
                      .lmMapSProxy2lMapProps( geoMapPropsProxy )
                      .noAttribution
                  )(

                    // Рендерим основную плитку карты.
                    ReactLeafletUtil.Tiles.OsmDefault,

                    // Плагин для геолокации текущего юзера.
                    LocateControlR(),

                    // Рендер шейпов и маркеров текущий узлов.
                    s.rcvrsGeoC { RcvrMarkersR(_)() }

                  )
                }

              }
            }

          )
        ),

        // Прицел для наведения. Пока не ясно, отображать его всегда или только когда карта перетаскивается.
        <.div(
          mapCSS.crosshair,
          HtmlConstants.PLUS
        )

      )
    }

  }


  val component = ScalaComponent.builder[Props]("SMap")
    .initialStateFromProps { mapInitProxy =>
      State(
        mmapC     = mapInitProxy.connect(_.state),
        rcvrsGeoC = mapInitProxy.connect(_.rcvrsGeo)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(mapInitProxy: Props) = component(mapInitProxy)

}
