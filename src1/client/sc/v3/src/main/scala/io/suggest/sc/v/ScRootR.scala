package io.suggest.sc.v

import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import com.materialui.{Mui, MuiColor, MuiList, MuiPalette, MuiPaletteAction, MuiPaletteBackground, MuiPaletteText, MuiPaletteTypes, MuiRawTheme, MuiThemeProvider, MuiThemeProviderProps, MuiThemeTypoGraphy, MuiToolBar, MuiToolBarProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.{MColorData, MColorTypes, MColors}
import io.suggest.common.empty.OptionUtil
import io.suggest.css.CssR
import io.suggest.i18n.MCommonReactCtx
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.r.CatchR
import io.suggest.react.{ReactCommonUtil, StyleProps}
import io.suggest.sc.m.MScRoot
import io.suggest.sc.m.grid.MGridS
import io.suggest.sc.m.inx._
import io.suggest.sc.m.menu.MMenuS
import io.suggest.sc.m.search.MSearchPanelS
import io.suggest.sc.v.dia.dlapp.{DlAppDiaR, DlAppMenuItemR}
import io.suggest.sc.v.dia.err.ScErrorDiaR
import io.suggest.sc.v.dia.first.WzFirstR
import io.suggest.sc.v.dia.settings.{ScSettingsDiaR, SettingsMenuItemR}
import io.suggest.sc.v.grid.GridR
import io.suggest.sc.v.hdr._
import io.suggest.sc.v.inx.{IndexSwitchAskR, WelcomeR}
import io.suggest.sc.v.menu._
import io.suggest.sc.v.search.{NodesFoundR, NodesSearchContR, STextR, SearchR}
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.spa.{FastEqUtil, OptFastEq}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, React, ScalaComponent}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 14:06
  * Description: Корневой react-компонент для выдачи третьего поколения.
  */
class ScRootR (
                gridR                   : GridR,
                searchR                 : SearchR,
                val sTextR              : STextR,
                val headerR             : HeaderR,
                menuR                   : MenuR,
                val enterLkRowR         : EnterLkRowR,
                val editAdR             : EditAdR,
                val aboutSioR           : AboutSioR,
                val welcomeR            : WelcomeR,
                val nodesSearchContR    : NodesSearchContR,
                val nodesFoundR         : NodesFoundR,
                rightR                  : RightR,
                val logoR               : LogoR,
                menuBtnR                : MenuBtnR,
                searchBtnR              : SearchBtnR,
                val hdrProgressR        : HdrProgressR,
                val indexSwitchAskR     : IndexSwitchAskR,
                val goBackR             : GoBackR,
                wzFirstR                : WzFirstR,
                dlAppMenuItemR          : DlAppMenuItemR,
                dlAppDiaR               : DlAppDiaR,
                settingsMenuItemR       : SettingsMenuItemR,
                scSettingsDiaR          : ScSettingsDiaR,
                commonReactCtx          : React.Context[MCommonReactCtx],
                scErrorDiaR             : ScErrorDiaR,
              ) {

  import io.suggest.sc.v.search.SearchCss.SearchCssFastEq
  import enterLkRowR.EnterLkRowRPropsValFastEq
  import editAdR.EditAdRPropsValFastEq
  import aboutSioR.AboutSioRPropsValFastEq
  import welcomeR.WelcomeRPropsValFastEq
  import io.suggest.sc.m.search.MScSearchText.MScSearchTextFastEq
  import logoR.LogoPropsValFastEq

  type Props = ModelProxy[MScRoot]


  protected[this] case class State(
                                    scCssArgsC                : ReactConnectProxy[ScCss],
                                    hdrPropsC                 : ReactConnectProxy[headerR.Props_t],
                                    wcPropsOptC               : ReactConnectProxy[Option[welcomeR.PropsVal]],
                                    enterLkRowC               : ReactConnectProxy[Option[enterLkRowR.PropsVal]],
                                    editAdC                   : ReactConnectProxy[Option[editAdR.PropsVal]],
                                    aboutSioC                 : ReactConnectProxy[aboutSioR.Props_t],
                                    searchSideBarC            : ReactConnectProxy[MSearchPanelS],
                                    menuOpenedSomeC           : ReactConnectProxy[Some[Boolean]],
                                    isRenderScC               : ReactConnectProxy[Some[Boolean]],
                                    colorsC                   : ReactConnectProxy[MColors],
                                    sTextC                    : ReactConnectProxy[sTextR.Props_t],
                                    nodesFoundC               : ReactConnectProxy[nodesFoundR.Props_t],
                                    nodesSearchContC          : ReactConnectProxy[nodesSearchContR.Props_t],
                                    hdrLogoOptC               : ReactConnectProxy[Option[logoR.PropsVal]],
                                    hdrOnGridBtnColorOptC     : ReactConnectProxy[Option[MColorData]],
                                    hdrProgressC              : ReactConnectProxy[hdrProgressR.Props_t],
                                    indexSwitchAskC           : ReactConnectProxy[indexSwitchAskR.Props_t],
                                    goBackC                   : ReactConnectProxy[goBackR.Props_t],
                                    commonReactCtxC           : ReactConnectProxy[MCommonReactCtx],
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
        mrootProxy.wrap(_.grid)(gridR.apply)( implicitly, MGridS.MGridSFastEq ),
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

        // Пункт скачивания мобильного приложения.
        dlAppMenuItemR( mrootProxy ),

        // Пункт открытия диалога с настройками выдачи.
        settingsMenuItemR.component( mrootProxy ),

      )
      val menuSideBarBody = mrootProxy.wrap(_.index.menu)( menuR(_)( menuSideBarBodyInner ) )( implicitly, MMenuS.MMenuSFastEq )

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
      val scCssComp = s.scCssArgsC { CssR.compProxied.apply }

      // Рендер провайдера тем MateriaUI, который заполняет react context.
      val muiThemeProviderComp = {
        val typographyProps = new MuiThemeTypoGraphy {
          override val useNextVariants = true
        }
        s.colorsC { mcolorsProxy =>
          val mcolors = mcolorsProxy.value
          val bgHex = mcolors.bg.getOrElse( MColorData(MColorTypes.Bg.scDefaultHex) ).hexCode
          val fgHex = mcolors.fg.getOrElse( MColorData(MColorTypes.Fg.scDefaultHex) ).hexCode
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

      // Финальный компонент: нельзя рендерить выдачу, если нет хотя бы минимальных данных для индекса.
      val sc = <.div(

        // Рендер ScCss.
        scCssComp,

        // В iOS 13 Safari вылетает ошибка при рендере. Пытаемся её перехватить:
        mrootProxy.wrap( _ => ScCssStatic.getClass.getName )( CatchR.component(_)(
          // css, который рендерится только один раз:
          mrootProxy.wrap(_ => ScCssStatic)( CssR.compProxied.apply )(implicitly, FastEq.AnyRefEq),
        )),

        // Рендер основного тела выдачи.
        s.isRenderScC { isRenderSomeProxy =>
          // Когда нет пока данных для рендера вообще, то ничего и не рендерить.
          ReactCommonUtil.maybeEl( isRenderSomeProxy.value.value )( muiThemeProviderComp )
        },

        // Всплывающая плашка для смены узла: TODO Запихнуть внутрь theme-provider?
        s.indexSwitchAskC { indexSwitchAskR.apply },

        // Диалог первого запуска.
        wzFirstR.component( mrootProxy ),

        // Диалог скачивания приложения.
        dlAppDiaR.component( mrootProxy ),

        // Плашка ошибки выдачи. Используем AnyRefEq (OptFeq.Plain) для ускорения: ошибки редки в общем потоке.
        mrootProxy.wrap(_.dialogs.error)( scErrorDiaR.apply )(implicitly, OptFastEq.Plain),

        // Диалог настроек.
        scSettingsDiaR.component( mrootProxy ),

      )

      // Зарегистрировать commonReact-контекст, чтобы подцепить динамический messages:
      s.commonReactCtxC { commonReactCtxProxy =>
        commonReactCtx.provide( commonReactCtxProxy.value )( sc )
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        scCssArgsC  = propsProxy.connect(_.index.scCss),

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

        enterLkRowC = propsProxy.connect { props =>
          for {
            scJsRouter <- props.internals.jsRouter.jsRouter.toOption
            if props.dev.platform.isBrowser
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
            if focusedData.info.canEdit
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
          OptionUtil.SomeBool( props.index.menu.opened )
        }( FastEq.AnyRefEq ),

        searchSideBarC = propsProxy.connect { props =>
          props.index.search.panel
        }( MSearchPanelS.MSearchPanelSFastEq ),

        isRenderScC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( !mroot.index.isFirstRun )
        }( FastEq.AnyRefEq ),

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

        indexSwitchAskC = propsProxy.connect { mroot =>
          mroot.index.state.switchAsk
        }( OptFastEq.Wrapped( MInxSwitchAskS.MInxSwitchAskSFastEq ) ) ,

        goBackC = propsProxy.connect { mroot =>
          OptionUtil.maybeOpt( !mroot.index.isAnyPanelOpened ) {
            mroot.index.state.prevNodeOpt
          }
        }( OptFastEq.Plain ),

        commonReactCtxC = propsProxy.connect( _.internals.info.commonReactCtx )( FastEq.AnyRefEq ),

      )
    }
    .renderBackend[Backend]
    .build

  def apply(scRootProxy: Props) = component( scRootProxy )

}
