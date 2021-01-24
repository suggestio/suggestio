package io.suggest.sc.v.search

import com.materialui.{MuiDrawerAnchor, MuiDrawerClasses, MuiModalProps, MuiSwipeableDrawer}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.{Css, CssR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.m.search._
import io.suggest.sc.m.MScRoot
import io.suggest.sc.v.search.found.NodesFoundR
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

import scala.scalajs.js

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

    private def _onSetOpen(opened: Boolean): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SideBarOpenClose(MScSideBars.Search, OptionUtil.SomeBool(opened)) )

    private val _onOpenCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      _onSetOpen( true )
    }
    private val _onCloseCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      _onSetOpen( false )
    }

    //private val _onSetOpenSearchSidebarF = ReactCommonUtil.cbFun1ToJsCb { opened: Boolean =>
    //  ReactDiodeUtil.dispatchOnProxyScopeCB( $, SideBarOpenClose(MScSideBars.Search, OptionUtil.SomeBool(opened)) )
    //}

    def render(mrootProxy: Props, s: State): VdomElement = {

      val nodesSearch = nodesFoundR.component( mrootProxy )

      val searchCss = s.searchCssC( CssR.compProxied.apply )

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
              geoMapOuterR.component(cssProxy)(
                tabContentInner
              )
            }

          )
        )
      }

      val _drawerCss = new MuiDrawerClasses {
        override val paper = ScCssStatic.Body.ovh.htmlClass
        override val modal = {
          val S = ScCssStatic.Search
          Css.flat(
            S.panel.htmlClass,
            S.modal.htmlClass,
          )
        }
      }
      val _anchorRight = MuiDrawerAnchor.right
      val _modalProps = new MuiModalProps {
        override val hideBackdrop = true
      }

      s.searchSideBarC { searchOpenedSomeProxy =>
        MuiSwipeableDrawer(
          new MuiSwipeableDrawer.Props {
            override val onOpen = _onOpenCbF
            override val onClose = _onCloseCbF
            override val open = searchOpenedSomeProxy.value.opened
            override val disableBackdropTransition = true
            override val transitionDuration = {
              if (mrootProxy.value.dev.platform.isUsingNow)
                js.undefined
              else
                js.defined( 0d )
            }
            override val anchor = _anchorRight
            override val classes = _drawerCss
            override val ModalProps = _modalProps
          }
        )(
          searchBarBody,
        )
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
    .renderBackend[Backend]
    .build

}
