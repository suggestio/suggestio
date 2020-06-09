package io.suggest.sc.v.search

import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import com.materialui.{MuiToolBar, MuiToolBarProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.{Css, CssR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil, StyleProps}
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.m.{MScReactCtx, MScRoot}
import io.suggest.sc.m.search._
import io.suggest.sc.v.{ScCss, ScCssStatic}
import io.suggest.sc.v.hdr.RightR
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, PropsChildren, React, ScalaComponent}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 14:24
  * Description: Компонент поисковой панели (живёт справа).
  */
class SearchR(
               val sTextR               : STextR,
               val nodesSearchContR     : NodesSearchContR,
               val nodesFoundR          : NodesFoundR,
               rightR                   : RightR,
               geoMapOuterR             : GeoMapOuterR,
               searchMapR               : SearchMapR,
               scReactCtxP              : React.Context[MScReactCtx],
             ) {

  import searchMapR.SearchMapRPropsValFastEq

  type Props = ModelProxy[MScRoot]


  protected[this] case class State(
                                    searchCssC                : ReactConnectProxy[SearchCss],
                                    searchSideBarC            : ReactConnectProxy[MSearchPanelS],
                                    sTextC                    : ReactConnectProxy[sTextR.Props_t],
                                    nodesFoundC               : ReactConnectProxy[nodesFoundR.Props_t],
                                    nodesSearchContC          : ReactConnectProxy[nodesSearchContR.Props_t],
                                  )


  class Backend( $: BackendScope[Props, State] ) {

    private val _onSetOpenSearchSidebarF = ReactCommonUtil.cbFun1ToJsCb { opened: Boolean =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SideBarOpenClose(MScSideBars.Search, opened) )
    }

    def render(mrootProxy: Props, s: State, children: PropsChildren): VdomElement = {

      // Наполнение контейнера поиска узлов:
      val nodeSearchInner = <.div(
        // Поисковое текстовое поле:
        MuiToolBar(
          new MuiToolBarProps {
            override val disableGutters = true
          }
        )(
          // Элементы поиска:
          s.sTextC { sTextR.apply },

          // Кнопка сворачивания:
          mrootProxy.wrap(_ => None)( rightR.apply )
        ),

        // Панель поиска: контент, зависимый от корневой модели:
        s.nodesFoundC { nodesFoundR.apply },
      )

      // Нода с единым скроллингом, передаваемая в children для SearchR:
      val searchBarChild = s.nodesSearchContC {
        nodesSearchContR(_)(
          nodeSearchInner
        )
      }

      // Непосредственно, панель поиска:
      val searchBarBody = scReactCtxP.consume { scReactCtx =>
        val scCss = scReactCtx.scCss
        val SearchCSS = scCss.Search

        // Рендер вкладки карты:
        val geoMap = mrootProxy.wrap { props =>
          val g = props.index.search.geo
          searchMapR.PropsVal(
            mapInit   = g.mapInit,
            searchCss = g.css
          )
        }( searchMapR.apply )

        // Тело текущего таба.
        val tabContentInner = <.div(
          // Содержимое вкладки с картой.
          ^.`class` := Css.Display.DISPLAY_BLOCK,
          // Списочек найденных элементов над картой (унесён в ScRoot, т.к. зависит от разных top-level-доступных моделей)
          searchBarChild,
          // Гео.карта:
          geoMap
        )

        <.div(
          ScCssStatic.Root.panelCommon,
          SearchCSS.panel,

          // Рендер очень динамической search-only css'ки:
          s.searchCssC { CssR.compProxied.apply },

          // Фон панели.
          <.div(
            ScCssStatic.Root.panelBg,
            scCss.bgColor
          ),

          // Наполнение панели.
          <.div(
            scCss.Search.content,

            // Контент вкладки, наконец.
            mrootProxy.wrap { props =>
              val geo = props.index.search.geo
              geoMapOuterR.PropsVal(
                searchCss     = geo.css,
                showCrossHair = true
              )
            } { cssProxy =>
              geoMapOuterR(cssProxy)(
                tabContentInner
              )
            }

          )
        )
      }

      // Значение "initial" для css-свойст.
      val css_initial = ScCss.css_initial

      val searchStyles = {
        val sidebarStyl = new StyleProps {
          override val zIndex    = ScCss.sideBarZIndex
          override val overflowY = css_initial
        }
        val contentStyl = new StyleProps {
          override val overflowY = css_initial
        }
        val overlayStyl = new StyleProps {
          override val zIndex = -100
        }
        new SidebarStyles {
          override val sidebar = sidebarStyl
          override val content = contentStyl
          override val overlay = overlayStyl
        }
      }

      s.searchSideBarC { searchOpenedSomeProxy =>
        // Используем react-sidebar вместо mui.SwipeableDrawer, т.к. последний конфиликтует с гео-картой на уровне touch-событий.
        Sidebar(
          new SidebarProps {
            override val sidebar      = searchBarBody.rawNode
            override val onSetOpen    = _onSetOpenSearchSidebarF
            override val open         = searchOpenedSomeProxy.value.opened
            override val transitions  = true
            override val touch        = true
            override val pullRight    = true
            override val shadow       = true
            override val styles       = searchStyles
          }
        )( children )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        searchCssC = propsProxy.connect(_.index.search.geo.css)( FastEq.AnyRefEq ),

        searchSideBarC = propsProxy.connect { props =>
          props.index.search.panel
        }( MSearchPanelS.MSearchPanelSFastEq ),

        sTextC = propsProxy.connect(_.index.search.text)( MScSearchText.MScSearchTextFastEq ),

        nodesSearchContC = propsProxy.connect { mroot =>
          mroot.index.search.geo.css
        }( SearchCss.SearchCssFastEq ),

        nodesFoundC = propsProxy.connect { mroot =>
          val geo = mroot.index.search.geo
          NodesFoundR.PropsVal(
            req                 = geo.found.req,
            hasMore             = geo.found.hasMore,
            selectedIds         = mroot.index.searchNodesSelectedIds,
            withDistanceToNull  = geo.mapInit.state.center,
            searchCss           = geo.css
          )
        }( NodesFoundR.NodesFoundRPropsValFastEq ),

      )
    }
    .renderBackendWithChildren[Backend]
    .build

}
