package io.suggest.sc.search.v

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.maps.m.MMapS
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.r.{LGeoMapR, RcvrMarkersR, ReactLeafletUtil}
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.search.m.{MScSearch, MScSearchText, MSearchTab, MSearchTabs}
import io.suggest.sc.styl.GetScCssF
import io.suggest.sjs.common.spa.OptFastEq
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import react.leaflet.control.LocateControlR
import io.suggest.sjs.common.spa.OptFastEq.Wrapped
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

  import MMapS.MMapSFastEq
  import MScSearchText.MScSearchTextFastEq

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
          s.mmapC { mmapS =>
            val mapCSS = CSS.Tabs.MapTab
            val lMapProps = LGeoMapR.lmMapSProxy2lMapProps( mmapS, mapCSS.geomap.htmlClass )

            s.tabC { currTabProxy =>
              val currTab = currTabProxy()

              <.div(
                mapCSS.outer,

                if (currTab == MSearchTabs.GeoMap)
                  ^.display.block
                else
                  ^.display.none,

                <.div(
                  mapCSS.wrapper,
                  <.div(
                    mapCSS.inner,

                    // Боремся с проблемой https://stackoverflow.com/a/36257493 с помощью отложенной инициализации.
                    s.isMapInitializedC { isMapInitializedSomeProxy =>
                      if (isMapInitializedSomeProxy.value.value) {
                        LMapR(lMapProps)(

                          // Рендерим основную плитку карты.
                          ReactLeafletUtil.Tiles.OsmDefaultNoAttrib,

                          // Плагин для геолокации текущего юзера.
                          LocateControlR(),

                          // Рендер шейпов и маркеров текущий узлов.
                          s.rcvrsGeoC( RcvrMarkersR(_)() )

                        )
                      } else {
                        ReactCommonUtil.VdomNullElement
                      }
                    }

                  )
                )
              )
            }
          }

        )
      }

    }

  }


  val component = ScalaComponent.builder[Props]("Search")
    .initialStateFromProps { propsProxy =>
      State(
        mmapC     = propsProxy.connect( _.mapState ),
        rcvrsGeoC = propsProxy.connect( _.rcvrsGeo ),
        textOptC  = propsProxy.connect( _.text ),
        tabC      = propsProxy.connect( _.currTab ),
        isShownC  = propsProxy.connect( p => Some(p.isShown) )( OptFastEq.PlainVal ),
        isMapInitializedC = propsProxy.connect(p => Some(p.isMapInitialized))( OptFastEq.PlainVal )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scSearchProxy: Props) = component( scSearchProxy )

}
