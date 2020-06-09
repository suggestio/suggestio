package io.suggest.sc.v

import com.github.balloob.react.sidebar.{Sidebar, SidebarProps, SidebarStyles}
import com.materialui.{Mui, MuiColor, MuiPalette, MuiPaletteAction, MuiPaletteBackground, MuiPaletteText, MuiPaletteTypes, MuiRawTheme, MuiThemeProvider, MuiThemeProviderProps, MuiThemeTypoGraphy, MuiToolBar, MuiToolBarProps}
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
import io.suggest.sc.m.search.MSearchPanelS
import io.suggest.sc.v.dia.dlapp.DlAppDiaR
import io.suggest.sc.v.dia.err.ScErrorDiaR
import io.suggest.sc.v.dia.first.WzFirstR
import io.suggest.sc.v.dia.settings.ScSettingsDiaR
import io.suggest.sc.v.grid.GridR
import io.suggest.sc.v.hdr._
import io.suggest.sc.v.inx.{IndexSwitchAskR, WelcomeR}
import io.suggest.sc.v.menu._
import io.suggest.sc.v.search.{NodesFoundR, NodesSearchContR, STextR, SearchR}
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.spa.{FastEqUtil, OptFastEq}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
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
                headerR                 : HeaderR,
                menuR                   : MenuR,
                val welcomeR            : WelcomeR,
                val nodesSearchContR    : NodesSearchContR,
                val nodesFoundR         : NodesFoundR,
                rightR                  : RightR,
                val indexSwitchAskR     : IndexSwitchAskR,
                wzFirstR                : WzFirstR,
                dlAppDiaR               : DlAppDiaR,
                scSettingsDiaR          : ScSettingsDiaR,
                commonReactCtx          : React.Context[MCommonReactCtx],
                scErrorDiaR             : ScErrorDiaR,
              ) {

  import io.suggest.sc.v.search.SearchCss.SearchCssFastEq
  import welcomeR.WelcomeRPropsValFastEq
  import io.suggest.sc.m.search.MScSearchText.MScSearchTextFastEq

  type Props = ModelProxy[MScRoot]


  protected[this] case class State(
                                    searchSideBarC            : ReactConnectProxy[MSearchPanelS],
                                    sTextC                    : ReactConnectProxy[sTextR.Props_t],
                                    nodesFoundC               : ReactConnectProxy[nodesFoundR.Props_t],
                                    nodesSearchContC          : ReactConnectProxy[nodesSearchContR.Props_t],
                                    scCssArgsC                : ReactConnectProxy[ScCss],
                                    wcPropsOptC               : ReactConnectProxy[Option[welcomeR.PropsVal]],
                                    isRenderScC               : ReactConnectProxy[Some[Boolean]],
                                    colorsC                   : ReactConnectProxy[MColors],
                                    indexSwitchAskC           : ReactConnectProxy[indexSwitchAskR.Props_t],
                                    commonReactCtxC           : ReactConnectProxy[MCommonReactCtx],
                                  )

  class Backend($: BackendScope[Props, State]) {

    private val _onSetOpenSearchSidebarF = ReactCommonUtil.cbFun1ToJsCb { opened: Boolean =>
      dispatchOnProxyScopeCB( $, SideBarOpenClose(MScSideBars.Search, opened) )
    }


    def render(mrootProxy: Props, s: State): VdomElement = {
      // Рендерим всё линейно, а не деревом, чтобы избежать вложенных connect.apply-фунцкий и сопутствующих эффектов.
      // Содержимое правой панели (панель поиска)
      // search (правый) sidebar.
      val scBody = <.div(
        // Ссылаемся на стиль.
        ScCssStatic.Root.root,

        // Экран приветствия узла:
        s.wcPropsOptC { welcomeR.apply },

        // заголовок выдачи:
        headerR.component( mrootProxy ),

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
      val css_initial = ScCss.css_initial

      val searchSideBar = {
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
          )( scBody )
        }
      }

      val menuSideBar = menuR.component( mrootProxy )( searchSideBar )

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

        indexSwitchAskC = propsProxy.connect { mroot =>
          mroot.index.state.switchAsk
        }( OptFastEq.Wrapped( MInxSwitchAskS.MInxSwitchAskSFastEq ) ) ,

        commonReactCtxC = propsProxy.connect( _.internals.info.commonReactCtx )( FastEq.AnyRefEq ),

      )
    }
    .renderBackend[Backend]
    .build

  def apply(scRootProxy: Props) = component( scRootProxy )

}
