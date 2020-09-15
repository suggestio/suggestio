package io.suggest.sc.v

import com.materialui.{MuiTheme, MuiThemeProvider, MuiThemeProviderProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.MColors
import io.suggest.common.empty.OptionUtil
import io.suggest.css.CssR
import io.suggest.i18n.MCommonReactCtx
import io.suggest.react.r.CatchR
import io.suggest.react.ReactCommonUtil
import io.suggest.routes.IJsRouter
import io.suggest.sc.m.MScRoot
import io.suggest.sc.m.grid.MGridS
import io.suggest.sc.v.dia.dlapp.DlAppDiaR
import io.suggest.sc.v.dia.first.WzFirstR
import io.suggest.sc.v.dia.login.ScLoginR
import io.suggest.sc.v.dia.nodes.ScNodesR
import io.suggest.sc.v.dia.settings.ScSettingsDiaR
import io.suggest.sc.v.grid.GridR
import io.suggest.sc.v.hdr._
import io.suggest.sc.v.inx.WelcomeR
import io.suggest.sc.v.menu._
import io.suggest.sc.v.search.SearchR
import io.suggest.sc.v.snack.ScSnacksR
import io.suggest.sc.v.styl.{ScCss, ScCssStatic, ScThemes}
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 14:06
  * Description: Корневой wrap-компонент react-выдачи.
  */
class ScRootR (
                // ниже - только DI
                gridR                   : GridR,
                searchR                 : SearchR,
                headerR                 : HeaderR,
                menuR                   : MenuR,
                welcomeR                : WelcomeR,
                wzFirstR                : WzFirstR,
                dlAppDiaR               : DlAppDiaR,
                scLoginR                : ScLoginR,
                scNodesR                : ScNodesR,
                scSettingsDiaR          : ScSettingsDiaR,
                scSnacksR               : ScSnacksR,
                scThemes                : ScThemes,
                crCtxP                  : React.Context[MCommonReactCtx],
                muiThemeCtxP            : React.Context[MuiTheme],
                scCssP                  : React.Context[ScCss],
                jsRouterOptP            : React.Context[Option[IJsRouter]],
              ) {

  type Props = ModelProxy[MScRoot]


  protected[this] case class State(
                                    scCssC                    : ReactConnectProxy[ScCss],
                                    isRenderScC               : ReactConnectProxy[Some[Boolean]],
                                    colorsC                   : ReactConnectProxy[MColors],
                                    commonReactCtxC           : ReactConnectProxy[MCommonReactCtx],
                                  // TODO Может есть более эффективный метод проброса? Роутер инициализируется при запуске системы, и больше не меняется.
                                    jsRouterOptC              : ReactConnectProxy[Option[IJsRouter]],
                                  )

  class Backend($: BackendScope[Props, State]) {

    def render(mrootProxy: Props, s: State): VdomElement = {
      // Рендерим всё линейно, а не деревом, чтобы избежать вложенных connect.apply-фунцкий и сопутствующих эффектов.
      // Содержимое правой панели (панель поиска)
      // search (правый) sidebar.
      // Левая панель меню:
      val menuSideBar = menuR.component( mrootProxy )(
        // Правая панель поиска:
        searchR.component( mrootProxy )(

          // заголовок выдачи:
          headerR.component( mrootProxy ),

          {
            // Основное тело выдачи:
            val scContent = <.div(
              // Ссылаемся на стиль.
              ScCssStatic.Root.root,

              // Экран приветствия узла:
              mrootProxy.wrap(_.index)( welcomeR.component.apply ),

              // Рендер плитки карточек узла:
              mrootProxy.wrap( _.grid )( gridR.component.apply )( implicitly, MGridS.MGridSFastEq ),
            )

            s.isRenderScC { isRenderSomeProxy =>
              scContent(
                // Когда нет пока данных для рендера вообще, то ничего и не рендерить.
                if (isRenderSomeProxy.value.value) ^.visibility.visible
                else ^.visibility.hidden,
              )
            }
          },
        )
      )

      // Финальный компонент: нельзя рендерить выдачу, если нет хотя бы минимальных данных для индекса.
      val sc = React.Fragment(

        // В iOS 13 Safari вылетает ошибка при рендере. Пытаемся её перехватить:
        mrootProxy.wrap( _ => ScCssStatic.getClass.getName )( CatchR.component(_)(
          // css, который рендерится только один раз:
          React.Fragment(
            CssR.component( ScCssStatic ),
            CssR.component( mrootProxy.value.dev.platformCss ),
            // Рендер стилей перед снаружи и перед остальной выдачей.
            // НЕЛЬЗЯ использовать react-sc-контекст, т.к. он не обновляется следом за scCss, т.к. остальным компонентам это не требуется.
            s.scCssC { CssR.compProxied.apply },
          )

        )),


        // Рендер провайдера тем MateriaUI, который заполняет react context.
        s.colorsC { mcolorsProxy =>
          val _theme = scThemes.muiDefault( mcolorsProxy.value )
          // Внешний react-контекст scala-уровня, т.к. не удалось понять, как достать MuiTheme из ThemeProvider-контекста.
          muiThemeCtxP.provide( _theme )(
            MuiThemeProvider(
              new MuiThemeProviderProps {
                override val theme = _theme
              }
            )(
              menuSideBar,
            ),

            // Диалог логина.
            mrootProxy.wrap( _.dialogs.login )( scLoginR.component.apply ),

          )
        },

        // Диалог управления узлами. Без темы, иначе дизайн сыплется.
        scNodesR.component( mrootProxy ),

        // Диалог первого запуска.
        wzFirstR.component( mrootProxy ),

        // Диалог настроек - требует scala-тему.
        scSettingsDiaR.component( mrootProxy ),

        // Диалог скачивания приложения - отображается только в браузере.
        ReactCommonUtil.maybeNode( mrootProxy.value.dev.platform.isDlAppAvail ) {
          dlAppDiaR.component( mrootProxy )
        },

        // snackbar
        scSnacksR.component( mrootProxy ),

      )

      // Организовать глобальные контексты:
      val scWithCrCtx = s.commonReactCtxC { commonReactCtxProxy =>
        crCtxP.provide( commonReactCtxProxy.value )( sc )
      }
      val scWithJsRouterCtx = s.jsRouterOptC { jsRouterOptProxy =>
        jsRouterOptP.provide( jsRouterOptProxy.value )(scWithCrCtx)
      }
      s.scCssC { scCssProxy =>
        scCssP.provide( scCssProxy.value )( scWithJsRouterCtx )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        scCssC = propsProxy.connect(_.index.scCss),

        isRenderScC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( !mroot.index.isFirstRun )
        }( FastEq.AnyRefEq ),

        colorsC = propsProxy.connect { mroot =>
          mroot.index.resp
            .fold(ScCss.COLORS_DFLT)(_.colors)
        }( FastEqUtil.AnyRefFastEq ),

        commonReactCtxC = propsProxy.connect( _.internals.info.commonReactCtx )( FastEq.AnyRefEq ),

        jsRouterOptC = propsProxy.connect( _.internals.jsRouter.jsRouterOpt ),

      )
    }
    .renderBackend[Backend]
    .build

}
