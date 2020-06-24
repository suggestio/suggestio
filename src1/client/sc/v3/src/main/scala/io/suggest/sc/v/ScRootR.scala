package io.suggest.sc.v

import com.materialui.{MuiThemeProvider, MuiThemeProviderProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.MColors
import io.suggest.common.empty.OptionUtil
import io.suggest.css.CssR
import io.suggest.i18n.MCommonReactCtx
import io.suggest.react.r.CatchR
import io.suggest.react.ReactCommonUtil
import io.suggest.sc.m.MScRoot
import io.suggest.sc.m.grid.MGridS
import io.suggest.sc.m.inx._
import io.suggest.sc.v.dia.dlapp.DlAppDiaR
import io.suggest.sc.v.dia.err.ScErrorDiaR
import io.suggest.sc.v.dia.first.WzFirstR
import io.suggest.sc.v.dia.settings.ScSettingsDiaR
import io.suggest.sc.v.grid.GridR
import io.suggest.sc.v.hdr._
import io.suggest.sc.v.inx.{IndexSwitchAskR, WelcomeR}
import io.suggest.sc.v.menu._
import io.suggest.sc.v.search.SearchR
import io.suggest.sc.v.styl.{ScCss, ScCssStatic, ScThemes}
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
                headerR                 : HeaderR,
                menuR                   : MenuR,
                welcomeR                : WelcomeR,
                indexSwitchAskR         : IndexSwitchAskR,
                wzFirstR                : WzFirstR,
                dlAppDiaR               : DlAppDiaR,
                scSettingsDiaR          : ScSettingsDiaR,
                scThemes                : ScThemes,
                commonReactCtx          : React.Context[MCommonReactCtx],
                scErrorDiaR             : ScErrorDiaR,
              ) {

  type Props = ModelProxy[MScRoot]


  protected[this] case class State(
                                    scCssArgsC                : ReactConnectProxy[ScCss],
                                    isRenderScC               : ReactConnectProxy[Some[Boolean]],
                                    colorsC                   : ReactConnectProxy[MColors],
                                    commonReactCtxC           : ReactConnectProxy[MCommonReactCtx],
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
          // Основное тело выдачи:
          <.div(
            // Ссылаемся на стиль.
            ScCssStatic.Root.root,

            // Экран приветствия узла:
            mrootProxy.wrap(_.index)( welcomeR.component.apply ),

            // заголовок выдачи:
            headerR.component( mrootProxy ),

            // Рендер плитки карточек узла:
            mrootProxy.wrap(_.grid)(gridR.apply)( implicitly, MGridS.MGridSFastEq ),
          )
        )
      )

      // Рендер провайдера тем MateriaUI, который заполняет react context.
      val muiThemeProviderComp = s.colorsC { mcolorsProxy =>
        MuiThemeProvider(
          new MuiThemeProviderProps {
            override val theme = scThemes.muiDefault( mcolorsProxy.value )
          }
        )(
          menuSideBar
        )
      }

      // Финальный компонент: нельзя рендерить выдачу, если нет хотя бы минимальных данных для индекса.
      val sc = React.Fragment(

        // Рендер стилей перед снаружи и перед остальной выдачей.
        // НЕЛЬЗЯ использовать react-sc-контекст, т.к. он не обновляется следом за scCss, т.к. остальным компонентам это не требуется.
        s.scCssArgsC { CssR.compProxied.apply },

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

        // Всплывающая плашка для смены узла:
        mrootProxy.wrap( _.index.state.switch )( indexSwitchAskR.component.apply )( implicitly, MInxSwitch.MInxSwitchFeq ),

        // Диалог первого запуска.
        wzFirstR.component( mrootProxy ),

        // Диалог скачивания приложения - отображается только в браузере.
        ReactCommonUtil.maybeNode( mrootProxy.value.dev.platform.isDlAppAvail ) {
          dlAppDiaR.component( mrootProxy )
        },

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

        isRenderScC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( !mroot.index.isFirstRun )
        }( FastEq.AnyRefEq ),

        colorsC = propsProxy.connect { mroot =>
          mroot.index.resp
            .fold(ScCss.COLORS_DFLT)(_.colors)
        }( FastEqUtil.AnyRefFastEq ),

        commonReactCtxC = propsProxy.connect( _.internals.info.commonReactCtx )( FastEq.AnyRefEq ),

      )
    }
    .renderBackend[Backend]
    .build

}
