package io.suggest.sc.search.v

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.maps.r.RcvrMarkersR
import io.suggest.sc.search.m._
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.spa.OptFastEq
import io.suggest.spa.OptFastEq.Wrapped
import japgolly.univeq._

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
               searchMapR : SearchMapR,
               getScCssF  : GetScCssF
             ) {

  import MScSearchText.MScSearchTextFastEq
  import MMapInitState.MMapInitStateFastEq

  type Props = ModelProxy[MScSearch]


  protected[this] case class State(
                                    mapInitC            : ReactConnectProxy[MMapInitState],
                                    rcvrsGeoC           : ReactConnectProxy[Pot[MGeoNodesResp]],
                                    textOptC            : ReactConnectProxy[Option[MScSearchText]],
                                    tabC                : ReactConnectProxy[MSearchTab],
                                    isShownC            : ReactConnectProxy[Some[Boolean]],
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
          s.tabC { currTabProxy =>
            <.div(
              if (currTabProxy.value ==* MSearchTabs.GeoMap)
                ^.display.block
              else
                ^.display.none,

              // Рендер карты:
              s.mapInitC { mapInitProxy =>
                searchMapR(mapInitProxy)(

                  // Рендер шейпов и маркеров текущий узлов.
                  s.rcvrsGeoC( RcvrMarkersR(_)() )

                )
              }
            )

          }

        )
      }

    }

  }


  val component = ScalaComponent.builder[Props]("Search")
    .initialStateFromProps { propsProxy =>
      State(
        mapInitC  = propsProxy.connect( _.mapInit ),
        rcvrsGeoC = propsProxy.connect( _.rcvrsGeo ),
        textOptC  = propsProxy.connect( _.text ),
        tabC      = propsProxy.connect( _.currTab ),
        isShownC  = propsProxy.connect( p => Some(p.isShown) )( OptFastEq.OptValueEq )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scSearchProxy: Props) = component( scSearchProxy )

}
