package io.suggest.sc.search.v

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.maps.m.{MGeoMapPropsR, MMapS, MapDragEnd, MapDragStart}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.r.{LGeoMapR, RcvrMarkersR, ReactLeafletUtil}
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.search.m.{MScSearch, MScSearchText, MSearchTab, MSearchTabs}
import io.suggest.sc.styl.GetScCssF
import io.suggest.sjs.leaflet.event.{DragEndEvent, Event}
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import react.leaflet.control.LocateControlR
import io.suggest.spa.OptFastEq
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import react.leaflet.lmap.LMapR

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 14:24
  * Description: Компонент поисковой панели (живёт справа).
  */
class SearchR(
               sTextR     : STextR,
               tabsR      : TabsR,
               getScCssF  : GetScCssF
             ) {

  import MGeoMapPropsR.MGeoMapPropsRFastEq
  import MScSearchText.MScSearchTextFastEq
  import MMapS.MMapSFastEq4Map

  type Props = ModelProxy[MScSearch]


  protected[this] case class State(
                                    mmapC               : ReactConnectProxy[MMapS],
                                    rcvrsGeoC           : ReactConnectProxy[Pot[MGeoNodesResp]],
                                    textOptC            : ReactConnectProxy[Option[MScSearchText]],
                                    tabC                : ReactConnectProxy[MSearchTab],
                                    isShownC            : ReactConnectProxy[Some[Boolean]],
                                    isMapInitializedC   : ReactConnectProxy[Some[Boolean]]
                                  )

  class Backend( $: BackendScope[Props, State] ) {

    private def _onMapDragStart(e: Event): Callback = {
      dispatchOnProxyScopeCB( $, MapDragStart )
    }
    private val _onMapDragStartF = ReactCommonUtil.cbFun1ToJsCb( _onMapDragStart )


    private def _onMapDragEnd(e: DragEndEvent): Callback = {
      dispatchOnProxyScopeCB( $, MapDragEnd(distancePx = e.distance) )
    }
    private val _onMapDragEndF = ReactCommonUtil.cbFun1ToJsCb( _onMapDragEnd )


    def render(s: State): VdomElement = {
      val scCss = getScCssF()
      val CSS = scCss.Search

      s.isShownC { isShownProxy =>
        <.div(
          CSS.panel,

          // Скрывать/показывать панель.
          if (isShownProxy().value) ^.display.block else ^.display.none,

          // Заголовок панели с кнопкой сокрытия.
          //<.div(
          //  CSS.panelHeader,
          //),

          // Рендер текстового поля поиска.
          s.textOptC { sTextR.apply },

          // Переключалка вкладок карта-теги
          s.tabC { tabsR.apply },

          // Карта.

          // Тело текущего таба.
            s.tabC { currTabProxy =>
              val mapCSS = CSS.Tabs.MapTab
              val currTab = currTabProxy()

              <.div(
                mapCSS.outer,

                if (currTab == MSearchTabs.GeoMap)
                  ^.display.block
                else
                  ^.display.none,

                <.div(
                  mapCSS.wrapper,
                  s.mmapC { mmapProxy =>
                    <.div(
                      mapCSS.inner,

                      // TODO Объеденить как-то isMapInitialized и MMapS? Например, сделать Pot[MMapS] вместо MMapS.
                      // Боремся с проблемой https://stackoverflow.com/a/36257493 с помощью отложенной инициализации.
                      s.isMapInitializedC { isMapInitializedSomeProxy =>
                        if (isMapInitializedSomeProxy.value.value) {
                          mmapProxy.wrap { m =>
                            MGeoMapPropsR(
                              center        = m.center,
                              zoom          = m.zoom,
                              locationFound = m.locationFound,
                              cssClass      = Some( mapCSS.geomap.htmlClass ),
                              onDragStart   = Some( _onMapDragStartF ),
                              onDragEnd     = Some( _onMapDragEndF )
                            )
                          } { geoMapPropsProxy =>
                            LMapR(
                              LGeoMapR.lmMapSProxy2lMapProps( geoMapPropsProxy )
                                .noAttribution
                            )(

                              // Рендерим основную плитку карты.
                              ReactLeafletUtil.Tiles.OsmDefault,

                              // Плагин для геолокации текущего юзера.
                              LocateControlR(),

                              // Рендер шейпов и маркеров текущий узлов.
                              s.rcvrsGeoC( RcvrMarkersR(_)() )

                            )
                          }
                        } else {
                          ReactCommonUtil.VdomNullElement
                        }
                      }

                    )
                  }
                ),

                // Прицел для наведения. Пока не ясно, отображать его всегда или только когда карта перетаскивается.
                //if (mmapProxy.value.dragging) {
                <.div(
                  mapCSS.crosshair,
                  HtmlConstants.PLUS
                )
                //} else {
                //  EmptyVdom
                //}

              )
            }

        )
      }

    }

  }


  val component = ScalaComponent.builder[Props]("Search")
    .initialStateFromProps { propsProxy =>
      State(
        mmapC = propsProxy.connect( _.mapState ),
        rcvrsGeoC = propsProxy.connect( _.rcvrsGeo ),
        textOptC  = propsProxy.connect( _.text ),
        tabC      = propsProxy.connect( _.currTab ),
        isShownC  = propsProxy.connect( p => Some(p.isShown) )( OptFastEq.OptValueEq ),
        isMapInitializedC = propsProxy.connect(p => Some(p.isMapInitialized))( OptFastEq.OptValueEq )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scSearchProxy: Props) = component( scSearchProxy )

}
