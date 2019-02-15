package io.suggest.sc.v

import chandu0101.scalajs.react.components.materialui.{Mui, MuiColor, MuiList, MuiPalette, MuiPaletteAction, MuiPaletteBackground, MuiPaletteText, MuiPaletteTypes, MuiRawTheme, MuiThemeProvider, MuiThemeProviderProps, MuiThemeTypoGraphy, MuiToolBar, MuiToolBarProps}
import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.{MColorData, MColors}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.CssR
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.{ReactCommonUtil, StyleProps}
import io.suggest.sc.m.MScRoot
import io.suggest.sc.m.grid.MGridS
import io.suggest.sc.m.inx._
import io.suggest.sc.m.search.MSearchPanelS
import io.suggest.sc.styl.{ScCss, ScCssStatic}
import io.suggest.sc.v.dia.first.WzFirstR
import io.suggest.sc.v.grid.GridR
import io.suggest.sc.v.hdr._
import io.suggest.sc.v.inx.{IndexSwitchAskR, WelcomeR}
import io.suggest.sc.v.menu._
import io.suggest.sc.v.search.{NodesFoundR, NodesSearchContR, STextR, SearchR}
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.spa.{FastEqUtil, OptFastEq}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
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
                val logoR               : LogoR,
                menuBtnR                : MenuBtnR,
                searchBtnR              : SearchBtnR,
                val hdrProgressR        : HdrProgressR,
                val geoLocR             : GeoLocR,
                val indexSwitchAskR     : IndexSwitchAskR,
                val goBackR             : GoBackR,
                val wzFirstR            : WzFirstR,
              ) {

  import io.suggest.sc.v.search.SearchCss.SearchCssFastEq
  import menuR.MenuRPropsValFastEq
  import enterLkRowR.EnterLkRowRPropsValFastEq
  import editAdR.EditAdRPropsValFastEq
  import aboutSioR.AboutSioRPropsValFastEq
  import welcomeR.WelcomeRPropsValFastEq
  import io.suggest.sc.m.search.MScSearchText.MScSearchTextFastEq
  import logoR.LogoPropsValFastEq

  type Props = ModelProxy[MScRoot]


  protected[this] case class State(
                                    scCssArgsC                : ReactConnectProxy[ScCss],
                                    gridPropsOptC             : ReactConnectProxy[gridR.Props_t],
                                    hdrPropsC                 : ReactConnectProxy[headerR.Props_t],
                                    wcPropsOptC               : ReactConnectProxy[Option[welcomeR.PropsVal]],
                                    enterLkRowC               : ReactConnectProxy[Option[enterLkRowR.PropsVal]],
                                    editAdC                   : ReactConnectProxy[Option[editAdR.PropsVal]],
                                    aboutSioC                 : ReactConnectProxy[aboutSioR.Props_t],
                                    searchSideBarC            : ReactConnectProxy[MSearchPanelS],
                                    menuC                     : ReactConnectProxy[menuR.PropsVal],
                                    menuOpenedSomeC           : ReactConnectProxy[Some[Boolean]],
                                    menuBlueToothOptC         : ReactConnectProxy[blueToothR.Props_t],
                                    dbgUnsafeOffsetsOptC      : ReactConnectProxy[unsafeScreenAreaOffsetR.Props_t],
                                    isRenderScC               : ReactConnectProxy[Some[Boolean]],
                                    colorsC                   : ReactConnectProxy[MColors],
                                    sTextC                    : ReactConnectProxy[sTextR.Props_t],
                                    nodesFoundC               : ReactConnectProxy[nodesFoundR.Props_t],
                                    nodesSearchContC          : ReactConnectProxy[nodesSearchContR.Props_t],
                                    hdrLogoOptC               : ReactConnectProxy[Option[logoR.PropsVal]],
                                    hdrOnGridBtnColorOptC     : ReactConnectProxy[Option[MColorData]],
                                    hdrProgressC              : ReactConnectProxy[hdrProgressR.Props_t],
                                    menuGeoLocC               : ReactConnectProxy[geoLocR.Props_t],
                                    indexSwitchAskC           : ReactConnectProxy[indexSwitchAskR.Props_t],
                                    goBackC                   : ReactConnectProxy[goBackR.Props_t],
                                    firstRunWzC               : ReactConnectProxy[wzFirstR.Props_t],
                                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onOpenSideBar(sideBar: MScSideBar, opened: Boolean): Callback =
      dispatchOnProxyScopeCB( $, SideBarOpenClose(sideBar, opened) )
    private def _mkSetOpenSideBarCbF(sideBar: MScSideBar) =
      ReactCommonUtil.cbFun1ToJsCb( _onOpenSideBar(sideBar, _: Boolean) )
    private val _onSetOpenSearchSidebarF = _mkSetOpenSideBarCbF( MScSideBars.Search )
    private val _onSetOpenMenuSidebarF = _mkSetOpenSideBarCbF( MScSideBars.Menu )


    def render(mrootProxy: Props, s: State): VdomElement = {
      // Сборка компонента заголовка выдачи:
      val hdr = {
        val hdrLogo       = s.hdrLogoOptC { logoR.apply }
        val hdrMenuBtn    = s.hdrOnGridBtnColorOptC { menuBtnR.apply }
        val hdrSearchBtn  = s.hdrOnGridBtnColorOptC { searchBtnR.apply }
        val hdrProgress   = s.hdrProgressC { hdrProgressR.apply }
        val hdrGoBack     = s.goBackC { goBackR.apply }

        // Компонент заголовка выдачи:
        s.hdrPropsC { hdrProxy =>
          headerR(hdrProxy)(
            hdrGoBack,
            hdrMenuBtn,

            hdrLogo,

            // -- Кнопки заголовка в зависимости от состояния выдачи --
            // Кнопки при нахождении в обычной выдаче без посторонних вещей:
            hdrSearchBtn,
            hdrProgress,
          )
        }
      }

      // Рендерим всё линейно, а не деревом, чтобы избежать вложенных connect.apply-фунцкий и сопутствующих эффектов.
      // Содержимое правой панели (панель поиска)
      // search (правый) sidebar.
      val scBody = <.div(
        // Ссылаемся на стиль.
        ScCssStatic.Root.root,

        // Экран приветствия узла:
        s.wcPropsOptC { welcomeR.apply },

        // заголовок выдачи:
        hdr,

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
      val searchBarBody = mrootProxy.wrap(_.index.search) {
        searchR(_)( searchBarChild )
      }

      // Значение "initial" для css-свойст.
      val css_initial = scalacss.internal.Literal.Typed.initial.value

      // z-index-костыли для расположения нескольких панелей и остального контента.
      // Жило оно одиноко в scCss, но пока унесено сюда после рефакторинга в DI/ScCss.
      val sideBarZIndex = 11

      val searchSideBar = {
        val searchStyles = {
          val sidebarStyl = new StyleProps {
            override val zIndex    = sideBarZIndex
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
          )( scBody )
        }
      }

      // Сборка панели меню:
      val menuSideBarBodyInner = MuiList()(
        // Строка входа в личный кабинет
        s.enterLkRowC { enterLkRowR.apply },

        // Кнопка редактирования карточки.
        s.editAdC { editAdR.apply },

        // Рендер кнопки "О проекте"
        s.aboutSioC { aboutSioR.apply },

        // Рендер переключателя геолокации
        s.menuGeoLocC { geoLocR.OnOffR.apply },

        // Рендер кнопки/подменю для управления bluetooth.
        s.menuBlueToothOptC { blueToothR.OnOffR.apply },

        // DEBUG: Если активна отладка, то вот это ещё отрендерить:
        s.dbgUnsafeOffsetsOptC { unsafeScreenAreaOffsetR.apply },
      )
      val menuSideBarBody = s.menuC { menuPropsProxy =>
        menuR( menuPropsProxy )( menuSideBarBodyInner )
      }

      val menuSideBar = {
        val menuSideBarStyles = {
          val zIndexBase = sideBarZIndex * 2
          val sidebarStyl = new StyleProps {
            override val zIndex    = zIndexBase
            override val overflowY = css_initial
          }
          val overlayStyl = new StyleProps {
            override val zIndex = zIndexBase - 1
          }
          new SidebarStyles {
            override val sidebar = sidebarStyl
            override val overlay = overlayStyl
          }
        }
        s.menuOpenedSomeC { menuOpenedSomeProxy =>
          Sidebar(
            new SidebarProps {
              override val sidebar      = menuSideBarBody.rawNode
              override val pullRight    = false
              override val touch        = true
              override val transitions  = true
              override val open         = menuOpenedSomeProxy.value.value
              override val onSetOpen    = _onSetOpenMenuSidebarF
              override val shadow       = true
              override val styles       = menuSideBarStyles
            }
          )( searchSideBar )
        }
      }

      // Рендер стилей перед снаружи и перед остальной выдачей.
      // НЕЛЬЗЯ использовать react-sc-контекст, т.к. он не обновляется следом за scCss, т.к. остальным компонентам это не требуется.
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
            override val primary      = primaryColor
            override val secondary    = secondaryColor
            // TODO Нужно портировать getLuminance() и выбирать dark/light на основе формулы luma >= 0.5. https://github.com/mui-org/material-ui/blob/355317fb479dc234c6b1e374428578717b91bdc0/packages/material-ui/src/styles/colorManipulator.js#L151
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
            menuSideBar
          )
        }
      }

      // Нормальный полный рендер выдачи:
      val fullRender = <.div(
        // Динамический css всей выдачи:
        scCssComp,
        muiThemeProviderComp,
      )

      // Финальный компонент: нельзя рендерить выдачу, если нет хотя бы минимальных данных для индекса.
      <.div(
        // css, который рендерится только один раз:
        mrootProxy.wrap(_ => ScCssStatic)( CssR.apply )(implicitly, FastEq.AnyRefEq),

        s.isRenderScC { isRenderSomeProxy =>
          // Когда нет пока данных для рендера вообще, то ничего и не рендерить.
          ReactCommonUtil.maybeEl( isRenderSomeProxy.value.value )( fullRender )
        },

        // Всплывающая плашка для смены узла: TODO Запихнуть внутрь theme-provider?
        s.indexSwitchAskC { indexSwitchAskR.apply },

        // Диалог первого запуска.
        s.firstRunWzC { wzFirstR.apply },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        scCssArgsC  = propsProxy.connect(_.index.scCss),

        gridPropsOptC = propsProxy.connect( _.grid )( MGridS.MGridSFastEq ),

        hdrPropsC = propsProxy.connect { props =>
          Some( props.index.resp.nonEmpty )
        }( OptFastEq.OptValueEq ),

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

        menuC = propsProxy.connect { props =>
          menuR.PropsVal(
            menuS = props.index.menu
          )
        },

        enterLkRowC = propsProxy.connect { props =>
          for {
            scJsRouter <- props.internals.jsRouter.jsRouter.toOption
          } yield {
            enterLkRowR.PropsVal(
              isLoggedIn      = props.internals.conf.isLoggedIn,
              // TODO А если текущий узел внутри карточки, то что тогда? Надо как-то по adn-типу фильтровать.
              isMyAdnNodeId   = props.index.state
                .rcvrId
                .filter { _ =>
                  props.index.resp.exists(_.isMyNode contains true)
                },
              scJsRouter = scJsRouter
            )
          }
        },

        editAdC = propsProxy.connect { props =>
          for {
            scJsRouter      <- props.internals.jsRouter.jsRouter.toOption
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
        }( FastEq.ValueEq ),

        dbgUnsafeOffsetsOptC = propsProxy.connect { mroot =>
          OptionUtil.maybe( mroot.internals.conf.debug ) {
            mroot.dev.screen.info.unsafeOffsets
          }
        }( OptFastEq.Plain ),

        isRenderScC = propsProxy.connect { mroot =>
          Some( !mroot.index.isFirstRun )
        }( OptFastEq.OptValueEq ),

        colorsC = propsProxy.connect { mroot =>
          mroot.index.resp
            .fold(ScCss.COLORS_DFLT)(_.colors)
        }( FastEqUtil.AnyRefFastEq ),

        sTextC = propsProxy.connect(_.index.search.text)( MScSearchTextFastEq ),

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

        nodesSearchContC = propsProxy.connect { mroot =>
          mroot.index.search.geo.css
        }( SearchCssFastEq ),

        hdrLogoOptC = propsProxy.connect { mroot =>
          for {
            mnode <- mroot.index.resp.toOption
          } yield {
            logoR.PropsVal(
              logoOpt     = mnode.logoOpt,
              nodeNameOpt = mnode.name,
              styled      = true,
            )
          }
        }( OptFastEq.Wrapped(LogoPropsValFastEq) ),

        hdrOnGridBtnColorOptC = propsProxy.connect { mroot =>
          OptionUtil.maybeOpt( !mroot.index.isAnyPanelOpened ) {
            mroot.index.resp
              .toOption
              .flatMap( _.colors.fg )
          }
        }( OptFastEq.Plain ),

        hdrProgressC = propsProxy.connect { mroot =>
          val r =
            mroot.index.resp.isPending ||
            mroot.grid.core.adsHasPending
          Some(r)
        }( OptFastEq.OptValueEq ),

        menuGeoLocC = propsProxy.connect { mroot =>
          mroot.dev.geoLoc.switch.onOff
        }( FastEqUtil.RefValFastEq ),

        indexSwitchAskC = propsProxy.connect { mroot =>
          mroot.index.state.switchAsk
        }( OptFastEq.Wrapped( MInxSwitchAskS.MInxSwitchAskSFastEq ) ) ,

        goBackC = propsProxy.connect { mroot =>
          OptionUtil.maybeOpt( !mroot.index.isAnyPanelOpened ) {
            mroot.index.state.prevNodeOpt
          }
        }( OptFastEq.Plain ),

        firstRunWzC = propsProxy.connect { mroot =>
          for (firstV <- mroot.dialogs.first.view) yield {
            wzFirstR.PropsVal(
              first      = firstV,
              fullScreen = mroot.dev.screen.info.isDialogWndFullScreen
            )
          }
        }( OptFastEq.Wrapped( wzFirstR.WzFirstRPropsValFastEq ) ),

      )
    }
    .renderBackend[Backend]
    .build

  def apply(scRootProxy: Props) = component( scRootProxy )

}
