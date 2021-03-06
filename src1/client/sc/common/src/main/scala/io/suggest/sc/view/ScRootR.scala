package io.suggest.sc.view

import com.materialui.MuiTheme
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.MColors
import io.suggest.common.empty.OptionUtil
import io.suggest.css.CssR
import io.suggest.i18n.MCommonReactCtx
import io.suggest.id.login.v.session.LogOutDiaR
import io.suggest.lk.u.MaterialUiUtil
import io.suggest.react.r.CatchR
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits.VdomElOptionExt
import io.suggest.routes.IJsRoutes
import io.suggest.sc.model.MScRoot
import io.suggest.sc.view.dia.dlapp.DlAppDiaR
import io.suggest.sc.view.dia.login.ScLoginR
import io.suggest.sc.view.dia.nodes.ScNodesR
import io.suggest.sc.view.dia.settings.ScSettingsDiaR
import io.suggest.sc.view.grid.{GridR, LocationButtonR}
import io.suggest.sc.view.hdr._
import io.suggest.sc.view.inx.WelcomeR
import io.suggest.sc.view.menu._
import io.suggest.sc.view.search.SearchR
import io.suggest.sc.view.snack.ScSnacksR
import io.suggest.sc.view.styl.{ScCss, ScCssStatic, ScThemes}
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
                gridR                   : GridR,
                searchR                 : SearchR,
                headerR                 : HeaderR,
                menuR                   : MenuR,
                welcomeR                : WelcomeR,
                dlAppDiaR               : DlAppDiaR,
                scLoginROpt             : Option[ScLoginR],
                scNodesROpt             : Option[ScNodesR],
                scSettingsDiaR          : ScSettingsDiaR,
                scSnacksROpt            : Option[ScSnacksR],
                scThemes                : ScThemes,
                logOutDiaROpt           : Option[LogOutDiaR],
                locationButtonR         : LocationButtonR,
                crCtxP                  : React.Context[MCommonReactCtx],
                muiThemeCtxP            : React.Context[MuiTheme],
                scCssP                  : React.Context[ScCss],
                jsRoutesOptP            : React.Context[Option[IJsRoutes]],
              ) {

  type Props = ModelProxy[MScRoot]


  protected[this] case class State(
                                    scCssC                    : ReactConnectProxy[ScCss],
                                    isRenderScC               : ReactConnectProxy[Some[Boolean]],
                                    colorsC                   : ReactConnectProxy[MColors],
                                    commonReactCtxC           : ReactConnectProxy[MCommonReactCtx],
                                    // TODO Может есть более эффективный метод проброса? Роутер инициализируется при запуске системы, и больше не меняется.
                                    jsRoutesOptC              : ReactConnectProxy[Option[IJsRoutes]],
                                  )

  class Backend($: BackendScope[Props, State]) {

    def render(mrootProxy: Props, s: State): VdomElement = {
      val scWithSideBars = React.Fragment(
        // Правая панель поиска:
        searchR.component( mrootProxy ),

        // Левая панель - панель меню:
        menuR.component( mrootProxy ),

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
            gridR.component( mrootProxy ),

            // Location floating button over grid.
            locationButtonR.component( mrootProxy ),
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

      // Финальный компонент: нельзя рендерить выдачу, если нет хотя бы минимальных данных для индекса.
      val sc = React.Fragment(

        // В iOS 13 Safari вылетает ошибка при рендере css. Пытаемся её перехватить, чтобы вся выдача не слетала:
        mrootProxy.wrap( _ => ScCssStatic.getClass.getName ) {
          CatchR.component(_)(
            // css, который рендерится только один раз:
            React.Fragment(
              CssR.component( ScCssStatic ),
              CssR.component( mrootProxy.value.dev.platformCss ),
              // Рендер стилей перед снаружи и перед остальной выдачей.
              // НЕЛЬЗЯ использовать react-sc-контекст, т.к. он не обновляется следом за scCss, т.к. остальным компонентам это не требуется.
              s.scCssC { CssR.compProxied.apply },
            )
          )
        },

        // Рендер провайдера тем MateriaUI, который заполняет react context.
        s.colorsC { mcolorsProxy =>
          val _theme = scThemes.muiDefault( mcolorsProxy.value )
          // Внешний react-контекст scala-уровня, т.к. не удалось понять, как достать MuiTheme из ThemeProvider-контекста.
          muiThemeCtxP.provide( _theme )(
            MaterialUiUtil.provideTheme( _theme )(
              scWithSideBars,
            )
          )
        },

        // Default-themed forms/dialogs. Theme-provider is mandatory, because PlatformComponents with iOS-style
        // needs withStyles(), that implicitly utilizing ThemeProvider context.
        MaterialUiUtil.provideTheme()(
          // Login form
          scLoginROpt.whenDefinedEl { scLoginR =>
            mrootProxy.wrap( _.dialogs.login )( scLoginR.component.apply )
          },
          // Snackbar
          scSnacksROpt.whenDefinedEl( _.component( mrootProxy ) ),
          // Settings dialog
          scSettingsDiaR.component( mrootProxy ),
          // Nodes management form.
          scNodesROpt.whenDefinedEl { scNodesR =>
            scNodesR.component( mrootProxy )
          },
          // Application download dialog: visible only in browser.
          ReactCommonUtil.maybeNode( mrootProxy.value.dev.platform.isDlAppAvail ) {
            dlAppDiaR.component( mrootProxy )
          },
          // Logout dialog
          logOutDiaROpt.whenDefinedEl { logOutDiaR =>
            mrootProxy.wrap( _.dialogs.login.logout )( logOutDiaR.component.apply )
          },
        ),

      )

      // Организовать глобальные контексты:
      val scWithCrCtx = s.commonReactCtxC { commonReactCtxProxy =>
        crCtxP.provide( commonReactCtxProxy.value )( sc )
      }
      val scWithJsRouterCtx = s.jsRoutesOptC { jsRouterOptProxy =>
        jsRoutesOptP.provide( jsRouterOptProxy.value )(scWithCrCtx)
      }
      val result = s.scCssC { scCssProxy =>
        scCssP.provide( scCssProxy.value )( scWithJsRouterCtx )
      }

      MaterialUiUtil.postprocessTopLevelOnlyStyles( result )
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
            .fold(ScCss.COLORS_DFLT)(_.resp.colors)
        }( FastEqUtil.AnyRefFastEq ),

        commonReactCtxC = propsProxy.connect( _.internals.info.reactCtx.context )( FastEq.AnyRefEq ),

        jsRoutesOptC = propsProxy.connect( _.internals.jsRouter.jsRoutesOpt ),

      )
    }
    .renderBackend[Backend]
    .build

}
