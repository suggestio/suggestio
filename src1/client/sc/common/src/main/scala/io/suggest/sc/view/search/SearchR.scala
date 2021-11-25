package io.suggest.sc.view.search

import com.materialui.{MuiDrawerAnchor, MuiDrawerClasses, MuiModalProps, MuiSwipeableDrawer}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.{Css, CssR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.model.inx.{MScSideBars, SideBarOpenClose}
import io.suggest.sc.model.search._
import io.suggest.sc.model.MScRoot
import io.suggest.sc.view.search.found.NodesFoundR
import io.suggest.sc.view.styl.{ScCss, ScCssStatic}
import io.suggest.sjs.common.empty.JsOptionUtil
import ReactCommonUtil.Implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
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
                     mkSearchMapF             : Option[ModelProxy[MGeoTabS] => VdomNode],
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

    //private val _onSetOpenSearchSidebarF = ReactCommonUtil.cbFun1ToJsCb( _onSetOpen )

    def render(mrootProxy: Props, s: State): VdomElement = {
      val nodesSearch = nodesFoundR.component( mrootProxy )

      val searchCss = s.searchCssC( CssR.compProxied.apply )

      // Непосредственно, панель поиска:
      // Рендер вкладки карты:
      val geoMap = mkSearchMapF.whenDefinedNode( _(mrootProxy.zoom(_.index.search.geo)) )

      // Тело текущего таба.
      val tabContentInner = <.div(
        // Содержимое вкладки с картой.
        ^.`class` := Css.Display.DISPLAY_BLOCK,
        // Списочек найденных элементов над картой (унесён в ScRoot, т.к. зависит от разных top-level-доступных моделей)
        nodesSearch,
        // Гео.карта:
        geoMap,
      )

      val tabContent = mrootProxy.wrap { props =>
        val geo = props.index.search.geo
        geoMapOuterR.PropsVal(
          searchCss     = geo.css,
          showCrossHair = true
        )
      } { cssProxy =>
        geoMapOuterR.component(cssProxy)(
          tabContentInner,
        )
      }: VdomNode

      val panelTagmod = TagMod(
        ScCssStatic.Root.panelCommon,
        ScCssStatic.Search.panel,

        // Рендер очень динамической search-only css'ки:
        searchCss,
      )
      val panelBgTagmod: TagMod = ScCssStatic.Root.panelBg

      val searchBarBody = scCssP.consume { scCss =>
        <.div(
          panelTagmod,

          // Фон панели.
          <.div(
            panelBgTagmod,
            scCss.panelBg,
          ),

          // Наполнение панели.
          <.div(
            scCss.Search.content,

            // Контент вкладки, наконец.
            tabContent,
          )
        )
      }

      // Реализация панели на базе MuiDrawer. Она лучше поддерживается и лучше вытаскивается из-за экрана.
      // Активировать, когда удасться изолировать touch-события карты от воздействия на MuiDrawer.
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

      s.searchSideBarC { searchOpenedSomeProxy =>
        val mroot = mrootProxy.value

        val _modalProps = new MuiModalProps {
          // Нужно узнать, сколько у нас есть места на экране на основе плитки. Отключение backdrop делает невозможным сокрытие панели взмахом.
          override val hideBackdrop = (mroot.grid.core.jdConf.gridColumnsCount > 3)
          override val keepMounted = true
        }

        val _animDurationU = JsOptionUtil.maybeDefined( !mroot.dev.platform.isUsingNow )( 0d )

        MuiSwipeableDrawer(
          new MuiSwipeableDrawer.Props {
            override val onOpen = _onOpenCbF
            override val onClose = _onCloseCbF
            override val open = searchOpenedSomeProxy.value.opened
            override val disableBackdropTransition = true
            // Тут по умолчанию не работает на iOS, хотя в кордове оно должно быть всегда. В браузере iOS Safari - надо тестировать.
            override val disableSwipeToOpen = false
            override val transitionDuration = _animDurationU
            override val anchor = _anchorRight
            override val classes = _drawerCss
            override val ModalProps = _modalProps
            override val swipeAreaWidth = 30
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
