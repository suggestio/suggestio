package io.suggest.sc.v.search

import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.{Css, CssR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil, StyleProps}
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.m.search._
import io.suggest.sc.m.MScRoot
import io.suggest.sc.v.search.found.NodesFoundR
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, PropsChildren, React, ScalaComponent}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 14:24
  * Description: Wrap-компонент поисковой панели (справа).
  */
final class SearchR(
                     nodesFoundR              : NodesFoundR,
                     geoMapOuterR             : GeoMapOuterR,
                     searchMapR               : SearchMapR,
                     scCssP                   : React.Context[ScCss],
                   ) {

  type Props = ModelProxy[MScRoot]


  case class State(
                    searchCssC                : ReactConnectProxy[SearchCss],
                    searchSideBarC            : ReactConnectProxy[MSearchPanelS],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private val _onSetOpenSearchSidebarF = ReactCommonUtil.cbFun1ToJsCb { opened: Boolean =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SideBarOpenClose(MScSideBars.Search, OptionUtil.SomeBool(opened)) )
    }

    def render(mrootProxy: Props, s: State, children: PropsChildren): VdomElement = {

      val nodesSearch = nodesFoundR.component( mrootProxy )

      val searchCss = s.searchCssC { CssR.compProxied.apply }

      // Непосредственно, панель поиска:
      val searchBarBody = scCssP.consume { scCss =>
        // Рендер вкладки карты:
        val geoMap = mrootProxy.wrap( _.index.search.geo )( searchMapR.component.apply )(implicitly, MGeoTabS.MGeoTabSFastEq)

        // Тело текущего таба.
        val tabContentInner = <.div(
          // Содержимое вкладки с картой.
          ^.`class` := Css.Display.DISPLAY_BLOCK,
          // Списочек найденных элементов над картой (унесён в ScRoot, т.к. зависит от разных top-level-доступных моделей)
          nodesSearch,
          // Гео.карта:
          geoMap,
        )

        <.div(
          ScCssStatic.Root.panelCommon,
          ScCssStatic.Search.panel,

          // Рендер очень динамической search-only css'ки:
          searchCss,

          // Фон панели.
          <.div(
            ScCssStatic.Root.panelBg,
            scCss.panelBg,
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

      )
    }
    .renderBackendWithChildren[Backend]
    .build

}
