package io.suggest.sc.search.v

import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
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
               sTextR         : STextR,
               tabsR          : TabsR,
               searchMapR     : SearchMapR,
               getScCssF      : GetScCssF,
               tagsSearchR    : TagsSearchR
             ) {

  import MScSearchText.MScSearchTextFastEq
  import MMapInitState.MMapInitStateFastEq
  import MTagsSearchS.MTagsSearchFastEq

  type Props = ModelProxy[MScSearch]


  protected[this] case class State(
                                    mapInitC            : ReactConnectProxy[MMapInitState],
                                    rcvrsGeoC           : ReactConnectProxy[Pot[MGeoNodesResp]],
                                    textOptC            : ReactConnectProxy[Option[MScSearchText]],
                                    tabC                : ReactConnectProxy[MSearchTab],
                                    isShownC            : ReactConnectProxy[Some[Boolean]],
                                  )


  /** Отрендерить css-класс, отвечающий за display:none или display:block в зависимости от значения флага. */
  private def _renderDisplayCss(isShown: Boolean): TagMod = {
    ^.`class` := (if (isShown) Css.Display.DISPLAY_BLOCK else Css.Display.HIDDEN)
  }

  class Backend( $: BackendScope[Props, State] ) {

    def render(props: Props, s: State): VdomElement = {
      val scCss = getScCssF()
      val SearchCSS = scCss.Search

      s.isShownC { isShownProxy =>
        <.div(
          SearchCSS.panel,

          // Скрывать/показывать панель.
          _renderDisplayCss( isShownProxy().value ),

          // Рендер текстового поля поиска.
          s.textOptC { sTextR.apply },

          // Переключалка вкладок карта-теги
          s.tabC { tabsR.apply },

          // Карта.

          // Тело текущего таба.
          s.tabC { currTabProxy =>
            val currTab = currTabProxy.value

            // Контейнер всех содержимых вкладок.
            <.div(

              // Содержимое вкладки с картой.
              <.div(
                _renderDisplayCss( currTab ==* MSearchTabs.GeoMap ),

                // Рендер карты:
                s.mapInitC { mapInitProxy =>
                  searchMapR(mapInitProxy)(

                    // Рендер шейпов и маркеров текущий узлов.
                    s.rcvrsGeoC( RcvrMarkersR(_)() )

                  )
                }
              ),

              // Содержимое вкладки с тегами.
              <.div(
                _renderDisplayCss( currTab ==* MSearchTabs.Tags ),

                props.wrap(_.tags) { tagsSearchR.apply }
              )

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
