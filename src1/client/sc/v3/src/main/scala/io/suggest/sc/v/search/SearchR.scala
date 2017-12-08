package io.suggest.sc.v.search

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.MColorData
import io.suggest.css.Css
import io.suggest.sc.m.search._
import io.suggest.sc.styl.GetScCssF
import io.suggest.sc.v.hdr.RightR
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.univeq._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.hdr.HSearchBtnClick

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
               rightR         : RightR,
               searchMapR     : SearchMapR,
               getScCssF      : GetScCssF,
               tagsSearchR    : TagsSearchR
             ) {

  import MMapInitState.MMapInitStateFastEq
  import MScSearchText.MScSearchTextFastEq
  import MTagsSearchS.MTagsSearchFastEq

  type Props = ModelProxy[MScSearch]


  protected[this] case class State(
                                    mapInitC            : ReactConnectProxy[MMapInitState],
                                    sTextC              : ReactConnectProxy[MScSearchText],
                                    tabC                : ReactConnectProxy[MSearchTab],
                                    isShownC            : ReactConnectProxy[Some[Boolean]],
                                  )


  /** Отрендерить css-класс, отвечающий за display:none или display:block в зависимости от значения флага. */
  private def _renderDisplayCss(isShown: Boolean): TagMod = {
    ^.`class` := (if (isShown) Css.Display.DISPLAY_BLOCK else Css.Display.HIDDEN)
  }

  class Backend( $: BackendScope[Props, State] ) {

    private def _onCloseClick: Callback = {
      dispatchOnProxyScopeCB($, HSearchBtnClick(open = false, silent = false))
    }

    def render(props: Props, s: State): VdomElement = {
      val scCss = getScCssF()
      val SearchCSS = scCss.Search

      <.div(
        SearchCSS.panel,

        // Рендер текстового поля поиска.
        s.sTextC { sTextR.apply },

        // Стрелка для сворачивания вкладки.
        props.wrap {_ => Option(MColorData.Examples.WHITE) } ( rightR.apply ),

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
              s.mapInitC { searchMapR.apply }
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


  val component = ScalaComponent.builder[Props]("Search")
    .initialStateFromProps { propsProxy =>
      State(
        mapInitC  = propsProxy.connect( _.mapInit ),
        sTextC    = propsProxy.connect( _.text ),
        tabC      = propsProxy.connect( _.currTab ),
        isShownC  = propsProxy.connect( p => Some(p.isShown) )( OptFastEq.OptValueEq )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scSearchProxy: Props) = component( scSearchProxy )

}
