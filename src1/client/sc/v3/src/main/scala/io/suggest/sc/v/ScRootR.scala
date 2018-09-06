package io.suggest.sc.v

import chandu0101.scalajs.react.components.materialui.{Mui, MuiColor, MuiPalette, MuiRawTheme, MuiThemeProvider, MuiThemeProviderProps}
import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.CssR
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.sc.m.MScRoot
import io.suggest.sc.m.hdr.{MHeaderStates, MenuOpenClose, SearchOpenClose}
import io.suggest.sc.m.search.MScSearch
import io.suggest.sc.styl.{GetScCssF, ScCss}
import io.suggest.sc.v.grid.GridR
import io.suggest.sc.v.hdr.HeaderR
import io.suggest.sc.v.inx.WelcomeR
import io.suggest.sc.v.search.{NodesFoundR, SearchR}
import io.suggest.spa.OptFastEq.Wrapped
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.{ReactCommonUtil, StyleProps}
import io.suggest.sc.v.menu._
import io.suggest.spa.{FastEqUtil, OptFastEq}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 14:06
  * Description: Корневой react-компонент для выдачи третьего поколения.
  */
class ScRootR (
                val gridR       : GridR,
                searchR         : SearchR,
                val headerR     : HeaderR,
                val menuR       : MenuR,
                val enterLkRowR : EnterLkRowR,
                val editAdR     : EditAdR,
                val aboutSioR   : AboutSioR,
                val welcomeR    : WelcomeR,
                val blueToothR  : BlueToothR,
                val unsafeScreenAreaOffsetR: UnsafeScreenAreaOffsetR,
                val nodesFoundR : NodesFoundR,
                getScCssF       : GetScCssF,
              ) {

  import MScSearch.MScSearchFastEq
  import gridR.GridPropsValFastEq
  import headerR.HeaderPropsValFastEq
  import menuR.MenuRPropsValFastEq
  import enterLkRowR.EnterLkRowRPropsValFastEq
  import editAdR.EditAdRPropsValFastEq
  import aboutSioR.AboutSioRPropsValFastEq
  import welcomeR.WelcomeRPropsValFastEq


  type Props = ModelProxy[MScRoot]

  protected[this] case class State(
                                    scCssArgsC          : ReactConnectProxy[ScCss],
                                    gridPropsOptC       : ReactConnectProxy[gridR.PropsVal],
                                    headerPropsC        : ReactConnectProxy[Option[headerR.PropsVal]],
                                    wcPropsOptC         : ReactConnectProxy[Option[welcomeR.PropsVal]],
                                    enterLkRowC         : ReactConnectProxy[Option[enterLkRowR.PropsVal]],
                                    editAdC             : ReactConnectProxy[Option[editAdR.PropsVal]],
                                    aboutSioC           : ReactConnectProxy[Option[aboutSioR.PropsVal]],
                                    searchC             : ReactConnectProxy[MScSearch],
                                    searchOpenedSomeC   : ReactConnectProxy[Some[Boolean]],
                                    menuC               : ReactConnectProxy[menuR.PropsVal],
                                    menuOpenedSomeC     : ReactConnectProxy[Some[Boolean]],
                                    menuBlueToothOptC   : ReactConnectProxy[blueToothR.Props_t],
                                    dbgUnsafeOffsetsOptC: ReactConnectProxy[unsafeScreenAreaOffsetR.Props_t],
                                    isRenderScC         : ReactConnectProxy[Some[Boolean]],
                                    searchNodesFoundC   : ReactConnectProxy[nodesFoundR.Props_t],
                                    colorsC             : ReactConnectProxy[MColors]
                                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onOpenSearchSidebar(opened: Boolean): Callback = {
      dispatchOnProxyScopeCB( $, SearchOpenClose(opened) )
    }
    private val _onOpenSearchSidebarF = ReactCommonUtil.cbFun1ToJsCb( _onOpenSearchSidebar )


    private def _onOpenMenuSidebar(opened: Boolean): Callback = {
      dispatchOnProxyScopeCB( $, MenuOpenClose(opened) )
    }
    private val _onOpenMenuSidebarF = ReactCommonUtil.cbFun1ToJsCb( _onOpenMenuSidebar )


    def render(s: State): VdomElement = {
      val scCss = getScCssF()

      // Рендерим всё линейно, а не деревом, чтобы избежать вложенных connect.apply-фунцкий и сопутствующих эффектов.
      // Содержимое правой панели (панель поиска)
      // search (правый) sidebar.
      val gridBody = <.div(
        // Ссылаемся на стиль.
        scCss.Root.root,

        // Экран приветствия узла:
        s.wcPropsOptC { welcomeR.apply },

        // Компонент заголовка выдачи:
        s.headerPropsC { headerR.apply },

        // Рендер плитки карточек узла:
        s.gridPropsOptC { gridR.apply }
      )

      // Панель поиска: контент, зависимый от корневой модели:
      val searchGeoNodesFound = s.searchNodesFoundC { nodesFoundR.apply }

      // Непосредственно, панель поиска:
      val searchBarBody = s.searchC {
        searchR(_)(
          searchGeoNodesFound
        )
      }


      val menuSideBarBodyInner = <.div(
        // Строка входа в личный кабинет
        s.enterLkRowC { enterLkRowR.apply },

        // Кнопка редактирования карточки.
        s.editAdC { editAdR.apply },

        // Рендер кнопки "О проекте"
        s.aboutSioC { aboutSioR.apply },

        // Рендер кнопки/подменю для управления bluetooth.
        s.menuBlueToothOptC { blueToothR.apply },

        // DEBUG: Если активна отладка, то вот это ещё отрендерить:
        s.dbgUnsafeOffsetsOptC { unsafeScreenAreaOffsetR.apply }
      )
      val menuSideBarBody = s.menuC { menuPropsProxy =>
        menuR( menuPropsProxy )( menuSideBarBodyInner )
      }

      val css_initial = scalacss.internal.Literal.Typed.initial.value

      val searchContentStyl = new StyleProps {
        override val zIndex    = scCss.Search.Z_INDEX
        override val overflowY = css_initial
      }
      val contentStyl = new StyleProps {
        override val overflowY = css_initial
      }
      val overlayStyl = new StyleProps {
        override val zIndex = -100
      }

      val searchStyles = new SidebarStyles {
        override val sidebar = searchContentStyl
        override val content = contentStyl
        override val overlay = overlayStyl
      }

      val searchSideBar = s.searchOpenedSomeC { searchOpenedSomeProxy =>
        Sidebar(
          new SidebarProps {
            override val sidebar      = searchBarBody.rawNode
            override val onSetOpen    = _onOpenSearchSidebarF
            override val open         = searchOpenedSomeProxy.value.value
            override val transitions  = true
            override val touch        = true
            override val pullRight    = true
            override val styles       = searchStyles
          }
        )( gridBody )
      }

      val menuSideBar = s.menuOpenedSomeC { menuOpenedSomeProxy =>
        Sidebar(
          new SidebarProps {
            override val sidebar      = menuSideBarBody.rawNode
            override val pullRight    = false
            override val touch        = true
            override val transitions  = true
            override val docked       = menuOpenedSomeProxy.value.value
            override val onSetOpen    = _onOpenMenuSidebarF
            override val shadow       = true
          }
        )( searchSideBar )
      }

      // Рендер стилей перед снаружи и перед остальной выдачей.
      val scCssComp = s.scCssArgsC { CssR.apply }

      // Рендер провайдера тем MateriaUI, который заполняет react context.
      val muiThemeProviderComp = s.colorsC { mcolorsProxy =>
        val mcolors = mcolorsProxy.value
        val bgHex = mcolors.bg.get.hexCode
        val fgHex = mcolors.fg.get.hexCode
        // Чтобы избежать Warning: Failed prop type: The following properties are not supported: `paletteRaw$1$f`. Please remove them.
        // тут расписывается весь JSON построчно:
        val primaryColor = new MuiColor {
          override val main = bgHex
          override val contrastText = fgHex
        }
        val secondaryColor = new MuiColor {
          override val main = fgHex
          override val contrastText = bgHex
        }
        val paletteRaw = new MuiPalette {
          override val primary = primaryColor
          override val secondary = secondaryColor
        }
        val themeRaw = new MuiRawTheme {
          override val palette = paletteRaw
        }
        val themeCreated = Mui.Styles.createMuiTheme( themeRaw )
        MuiThemeProvider(
          new MuiThemeProviderProps {
            override val theme = themeCreated
          }
        )( menuSideBar )
      }

      // Финальный компонент: нельзя рендерить выдачу, если нет хотя бы минимальных данных для индекса.
      s.isRenderScC { isRenderSomeProxy =>
        if (isRenderSomeProxy.value.value) {
          <.div(
            scCssComp,
            muiThemeProviderComp
          )
        } else {
          // Нет пока данных для рендера вообще
          <.div
        }
      }

    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        scCssArgsC  = propsProxy.connect(_.index.scCss),

        gridPropsOptC = propsProxy.connect { mroot =>
          gridR.PropsVal(
            grid    = mroot.grid,
            fgColor = mroot.index.resp
              .toOption
              .flatMap(_.colors.fg)
          )
        },

        headerPropsC = propsProxy.connect { props =>
          for {
            resp <- props.index.resp.toOption
          } yield {
            headerR.PropsVal(
              hdrState  = if (props.index.search.isShown) {
                MHeaderStates.Search
              } else if (props.index.menu.opened) {
                MHeaderStates.Menu
              } else {
                MHeaderStates.PlainGrid
              },
              node      = resp
            )
          }
        },

        wcPropsOptC = propsProxy.connect { props =>
          for {
            resp    <- props.index.resp.toOption
            wcInfo  <- resp.welcome
            wcState <- props.index.welcome
          } yield {
            welcomeR.PropsVal(
              wcInfo   = wcInfo,
              nodeName = resp.name,
              state    = wcState
            )
          }
        },

        searchC = propsProxy.connect(_.index.search),

        menuC = propsProxy.connect { props =>
          menuR.PropsVal(
            menuS = props.index.menu
          )
        },

        enterLkRowC = propsProxy.connect { props =>
          for {
            scJsRouter <- props.internals.jsRouter.toOption
          } yield {
            enterLkRowR.PropsVal(
              isLoggedIn      = props.internals.conf.isLoggedIn,
              // TODO А если текущий узел внутри карточки, то что тогда? Надо как-то по adn-типу фильтровать.
              isMyAdnNodeId   = props.index.state
                .currRcvrId
                .filter { _ =>
                  props.index.resp.exists(_.isMyNode)
                },
              scJsRouter = scJsRouter
            )
          }
        },

        editAdC = propsProxy.connect { props =>
          for {
            scJsRouter      <- props.internals.jsRouter.toOption
            focusedAdOuter  <- props.grid.core.focusedAdOpt
            focusedData     <- focusedAdOuter.focused.toOption
            if focusedData.canEdit
            focusedAdId     <- focusedAdOuter.nodeId
          } yield {
            editAdR.PropsVal(
              adId      = focusedAdId,
              scRoutes  = scJsRouter
            )
          }
        },

        aboutSioC = propsProxy.connect { props =>
          val propsVal = aboutSioR.PropsVal(
            aboutNodeId = props.internals.conf.aboutSioNodeId
          )
          Some(propsVal)
        },

        menuOpenedSomeC = propsProxy.connect { props =>
          Some( props.index.menu.opened )
        }( OptFastEq.OptValueEq ),

        searchOpenedSomeC = propsProxy.connect { props =>
          Some( props.index.search.isShown )
        }( OptFastEq.OptValueEq ),

        menuBlueToothOptC = propsProxy.connect { mroot =>
          mroot.dev.beaconer.isEnabled
        },

        dbgUnsafeOffsetsOptC = propsProxy.connect { mroot =>
          OptionUtil.maybe( mroot.internals.conf.debug ) {
            mroot.dev.screen.info.unsafeOffsets
          }
        }( OptFastEq.Plain ),

        isRenderScC = propsProxy.connect { mroot =>
          Some( mroot.index.resp.nonEmpty )
        }( OptFastEq.OptValueEq ),

        searchNodesFoundC = propsProxy.connect { mroot =>
          val geo = mroot.index.search.geo
          nodesFoundR.PropsVal(
            req             = geo.found.req,
            hasMore         = geo.found.hasMore,
            selectedIds     = mroot.index.searchNodesSelectedIds,
            withDistanceTo  = geo.mapInit.state.center,
            searchCss       = geo.css
          )
        },

        colorsC = propsProxy.connect { mroot =>
          mroot.index.resp
            .fold(ScCss.COLORS_DFLT)(_.colors)
        }( FastEqUtil.AnyRefFastEq )

      )
    }
    .renderBackend[Backend]
    .build

  def apply(scRootProxy: Props) = component( scRootProxy )

}
