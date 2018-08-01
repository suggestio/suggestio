package io.suggest.sc.v.search

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.MColorData
import io.suggest.css.Css
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.m.search._
import io.suggest.sc.search.{MSearchTab, MSearchTabs}
import io.suggest.sc.styl.GetScCssF
import io.suggest.sc.v.hdr.RightR
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.univeq._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 14:24
  * Description: Компонент поисковой панели (живёт справа).
  */
class SearchR(
               sTextR             : STextR,
               tabsR              : TabsR,
               rightR             : RightR,
               searchMapR         : SearchMapR,
               getScCssF          : GetScCssF,
               val nodesFoundR    : NodesFoundR,
               tagsSearchR        : TagsSearchR
             ) {

  import MMapInitState.MMapInitStateFastEq
  import MScSearchText.MScSearchTextFastEq
  import nodesFoundR.NodesFoundRPropsValFastEq

  type Props = ModelProxy[MScSearch]


  protected[this] case class State(
                                    sTextC              : ReactConnectProxy[MScSearchText],
                                    tabC                : ReactConnectProxy[MSearchTab],
                                    isShownC            : ReactConnectProxy[Some[Boolean]],
                                    nodesFoundC         : ReactConnectProxy[nodesFoundR.Props_t],
                                    tagsFoundC          : ReactConnectProxy[nodesFoundR.Props_t],
                                  )


  /** Отрендерить css-класс, отвечающий за display:none или display:block в зависимости от значения флага. */
  private def _renderDisplayCss(isShown: Boolean): TagMod = {
    ^.`class` := (if (isShown) Css.Display.DISPLAY_BLOCK else Css.Display.HIDDEN)
  }

  class Backend( $: BackendScope[Props, State] ) {

    def render(props: Props, s: State): VdomElement = {
      val scCss = getScCssF()
      val SearchCSS = scCss.Search

      // Рендер вкладки карты:
      val geoTabMap = props.wrap(_.geo.mapInit) { searchMapR.apply }

      val geoNodesFound = s.nodesFoundC { nodesFoundProxy =>
        val v = nodesFoundProxy.value
        ReactCommonUtil.maybeEl( v.req.nonEmpty ) {
          nodesFoundR(nodesFoundProxy)
        }
      }

      // Рендер наполнения вкладки тегов:
      val tagsTab = tagsSearchR( props )(
        s.tagsFoundC { nodesFoundR.apply }
      )

      <.div(
        SearchCSS.panel,

        // Фон панели.
        <.div(
          scCss.Root.panelBg
        ),

        // Наполнение панели.
        <.div(
          scCss.Search.content,

          // Рендер текстового поля поиска.
          s.sTextC { sTextR.apply },

          // Стрелка для сворачивания вкладки.
          props.wrap {_ => Option(MColorData.Examples.WHITE) } ( rightR.applyReusable ),

          // Переключалка вкладок карта-теги
          s.tabC { tabsR.apply },

          // Тело текущего таба.
          // TODO Это должно разруливаться полностью на уровне CSS. Но не ScCss, а где-то ещё. Там же в CSS и анимироваться.
          s.tabC { currTabProxy =>
            val currTab = currTabProxy.value

            // Контейнер всех содержимых вкладок.
            <.div(
              // Содержимое вкладки с картой.
              <.div(
                _renderDisplayCss( currTab ==* MSearchTabs.GeoMap ),
                // Списочек найденных элементов над картой:
                geoNodesFound,
                // Гео.карта:
                geoTabMap
              ),

              // Содержимое вкладки с тегами.
              <.div(
                _renderDisplayCss( currTab ==* MSearchTabs.Tags ),
                tagsTab
              )
            )

          }

        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        sTextC    = propsProxy.connect( _.text ),
        tabC      = propsProxy.connect( _.currTab ),
        isShownC  = propsProxy.connect( p => Some(p.isShown) )( OptFastEq.OptValueEq ),
        nodesFoundC = propsProxy.connect { msearch =>
          nodesFoundR.PropsVal(
            req             = msearch.geo.found.req,
            hasMore         = false,
            selectedId      = None,    // TODO currRcvrId. Надо вынести коннекшены в top-level ScRoot.
            withDistanceTo  = msearch.geo.mapInit.userLoc,
            onTab           = MSearchTabs.GeoMap
          )
        },
        tagsFoundC = propsProxy.connect { msearch =>
          nodesFoundR.PropsVal(
            req             = msearch.tags.req,
            hasMore         = msearch.tags.hasMore,
            selectedId      = msearch.tags.selectedId,
            withDistanceTo  = None,
            onTab           = MSearchTabs.Tags
          )
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scSearchProxy: Props) = component( scSearchProxy )

}
