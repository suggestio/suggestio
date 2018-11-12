package io.suggest.sc.v

import chandu0101.scalajs.react.components.materialui.{Mui, MuiColor, MuiDrawerAnchors, MuiList, MuiPalette, MuiPaletteAction, MuiPaletteBackground, MuiPaletteText, MuiPaletteTypes, MuiRawTheme, MuiSwipeableDrawer, MuiSwipeableDrawerProps, MuiThemeProvider, MuiThemeProviderProps, MuiThemeTypoGraphy, MuiToolBar, MuiToolBarProps}
import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.CssR
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.sc.m.MScRoot
import io.suggest.sc.m.hdr.MHeaderStates
import io.suggest.sc.m.search.{MScSearch, MSearchPanelS}
import io.suggest.sc.styl.{GetScCssF, ScCss}
import io.suggest.sc.v.grid.GridR
import io.suggest.sc.v.hdr.{HeaderR, RightR}
import io.suggest.sc.v.inx.WelcomeR
import io.suggest.sc.v.search.{NodesFoundR, NodesSearchContR, STextR, SearchR}
import io.suggest.spa.OptFastEq.Wrapped
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent, ScalaComponent}
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.{ReactCommonUtil, StyleProps}
import io.suggest.sc.m.inx._
import io.suggest.sc.v.menu._
import io.suggest.sc.v.search.SearchCss.SearchCssFastEq
import io.suggest.spa.{FastEqUtil, OptFastEq}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 14:06
  * Description: Корневой react-компонент для выдачи третьего поколения.
  */
class ScRootR (
                val gridR               : GridR,
                searchR                 : SearchR,
                val sTextR              : STextR,
                val headerR             : HeaderR,
                val menuR               : MenuR,
                val enterLkRowR         : EnterLkRowR,
                val editAdR             : EditAdR,
                val aboutSioR           : AboutSioR,
                val welcomeR            : WelcomeR,
                val blueToothR          : BlueToothR,
                val unsafeScreenAreaOffsetR: UnsafeScreenAreaOffsetR,
                val nodesSearchContR    : NodesSearchContR,
                val nodesFoundR         : NodesFoundR,
                rightR                  : RightR,
                getScCssF               : GetScCssF,
              ) {

  import MScSearch.MScSearchFastEq
  import gridR.GridPropsValFastEq
  import headerR.HeaderPropsValFastEq
  import menuR.MenuRPropsValFastEq
  import enterLkRowR.EnterLkRowRPropsValFastEq
  import editAdR.EditAdRPropsValFastEq
  import aboutSioR.AboutSioRPropsValFastEq
  import welcomeR.WelcomeRPropsValFastEq
  import io.suggest.sc.m.search.MScSearchText.MScSearchTextFastEq

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
                                    searchSideBarC      : ReactConnectProxy[MSearchPanelS],
                                    menuC               : ReactConnectProxy[menuR.PropsVal],
                                    menuOpenedSomeC     : ReactConnectProxy[Some[Boolean]],
                                    menuBlueToothOptC   : ReactConnectProxy[blueToothR.Props_t],
                                    dbgUnsafeOffsetsOptC: ReactConnectProxy[unsafeScreenAreaOffsetR.Props_t],
                                    isRenderScC         : ReactConnectProxy[Some[Boolean]],
                                    colorsC             : ReactConnectProxy[MColors],
                                    sTextC              : ReactConnectProxy[sTextR.Props_t],
                                    nodesFoundC         : ReactConnectProxy[nodesFoundR.Props_t],
                                    nodesSearchContC    : ReactConnectProxy[nodesSearchContR.Props_t],
                                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onOpenSideBar(sideBar: MScSideBar, opened: Boolean): Callback =
      dispatchOnProxyScopeCB( $, SideBarOpenClose(sideBar, opened) )
    private val _onSetOpenSearchSidebarF = ReactCommonUtil.cbFun1ToJsCb( _onOpenSideBar(MScSideBars.Search, _: Boolean) )

    //private val _onOpenMenuSidebarF = ReactCommonUtil.cbFun1ToJsCb( _onOpenSideBar(MScSideBars.Menu, _: Boolean) )
    private def _mkSetOpenSideBarCbF(sboc: => SideBarOpenClose) = {
      ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
        dispatchOnProxyScopeCB( $, sboc )
      }
    }


    def render(mrootProxy: Props, s: State): VdomElement = {
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
      val searchBarBody = s.searchC {
        searchR(_)( searchBarChild )
      }

      /*
      // TODO Не работает нормально сворачивание search-панели, конфликтует с гео-картой.
      val searchSideBar2 = {
        val _onOpen  = _mkSetOpenSideBarCbF( SideBarOpenClose(MScSideBars.Search, open = true) )
        val _onClose = _mkSetOpenSideBarCbF( SideBarOpenClose(MScSideBars.Search, open = false) )
        s.searchSideBarC { stateProxy =>
          val s = stateProxy.value
          MuiSwipeableDrawer(
            new MuiSwipeableDrawerProps {
              override val open    = s.opened
              override val onOpen  = _onOpen
              override val onClose = _onClose
              override val anchor  = MuiDrawerAnchors.right
              override val disableBackdropTransition = true
              override val hideBackdrop = true
              // При таскании карты нужно запрещать свайпинг. Через stopPropagation() это сделать нельзя, т.к. в хроме нельзя прерывать touchmove, а touchstart просто игнорируется в mui.
              override val disableDiscovery = s.fixed
              override val disableSwipeToOpen = s.fixed
              override val variant = MuiDrawerVariants.temporary
            }
          )( searchBarBody )
        }
      }
      */

      val searchStyles = {
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
        new SidebarStyles {
          override val sidebar = searchContentStyl
          override val content = contentStyl
          override val overlay = overlayStyl
        }
      }

      val searchSideBar = s.searchSideBarC { searchOpenedSomeProxy =>
        // Используем react-sidebar вместо mui.SwipeableDrawer, т.к. последний конфиликтует с гео-картой на уровне touch-событий.
        Sidebar(
          new SidebarProps {
            override val sidebar      = searchBarBody.rawNode
            override val onSetOpen    = _onSetOpenSearchSidebarF
            override val open         = searchOpenedSomeProxy.value.opened
            override val transitions  = true
            override val touch        = true
            override val pullRight    = true
            override val styles       = searchStyles
          }
        )( gridBody )
      }

      // Сборка панели меню:
      val menuSideBarBodyInner = MuiList()(
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
      val menuSideBar2 = {
        val _onOpen  = _mkSetOpenSideBarCbF( SideBarOpenClose(MScSideBars.Menu, open = true) )
        val _onClose = _mkSetOpenSideBarCbF( SideBarOpenClose(MScSideBars.Menu, open = false) )
        s.menuOpenedSomeC { menuOpenedSomeProxy =>
          MuiSwipeableDrawer(
            new MuiSwipeableDrawerProps {
              override val open     = menuOpenedSomeProxy.value.value
              override val onOpen   = _onOpen
              override val onClose  = _onClose
              override val anchor   = MuiDrawerAnchors.left
            }
          )( menuSideBarBody )
        }
      }

      // Рендер стилей перед снаружи и перед остальной выдачей.
      val scCssComp = s.scCssArgsC { CssR.apply }

      // Рендер провайдера тем MateriaUI, который заполняет react context.
      val muiThemeProviderComp = {
        val typographyProps = new MuiThemeTypoGraphy {
          override val useNextVariants = true
        }
        s.colorsC { mcolorsProxy =>
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
          val paletteText = new MuiPaletteText {
            override val primary = fgHex
            override val secondary = bgHex
          }
          val palletteBg = new MuiPaletteBackground {
            override val paper = bgHex
            override val default = bgHex
          }
          val btnsPalette = new MuiPaletteAction {
            override val active = fgHex
          }
          val paletteRaw = new MuiPalette {
            //override val common = paletteCommon
            override val primary      = primaryColor
            override val secondary    = secondaryColor
            override val `type`       = MuiPaletteTypes.dark
            override val text         = paletteText
            override val background   = palletteBg
            override val action       = btnsPalette
          }
          val themeRaw = new MuiRawTheme {
            override val palette = paletteRaw
            override val typography = typographyProps
            //override val shadows = js.Array("none")
          }
          val themeCreated = Mui.Styles.createMuiTheme( themeRaw )
          //println( JSON.stringify(themeCreated) )
          MuiThemeProvider(
            new MuiThemeProviderProps {
              override val theme = themeCreated
            }
          )(
            menuSideBar2,
            searchSideBar,
          )
        }
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
              hdrState  = if (props.index.search.panel.opened) {
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

        searchSideBarC = propsProxy.connect { props =>
          props.index.search.panel
        }( MSearchPanelS.MSearchPanelSFastEq ),

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

        colorsC = propsProxy.connect { mroot =>
          mroot.index.resp
            .fold(ScCss.COLORS_DFLT)(_.colors)
        }( FastEqUtil.AnyRefFastEq ),

        sTextC = propsProxy.connect(_.index.search.text)( MScSearchTextFastEq ),

        nodesFoundC = propsProxy.connect { mroot =>
          val geo = mroot.index.search.geo
          NodesFoundR.PropsVal(
            req             = geo.found.req,
            hasMore         = geo.found.hasMore,
            selectedIds     = mroot.index.searchNodesSelectedIds,
            withDistanceTo  = geo.mapInit.state.center,
            searchCss       = geo.css
          )
        }( NodesFoundR.NodesFoundRPropsValFastEq ),

        nodesSearchContC = propsProxy.connect { mroot =>
          mroot.index.search.geo.css
        }( SearchCssFastEq ),

      )
    }
    .renderBackend[Backend]
    .build

  def apply(scRootProxy: Props) = component( scRootProxy )

}
