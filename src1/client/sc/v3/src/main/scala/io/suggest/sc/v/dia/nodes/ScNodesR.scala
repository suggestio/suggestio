package io.suggest.sc.v.dia.nodes

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.LkNodesFormCircuit
import io.suggest.lk.nodes.form.r.LkNodesFormR
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.m.{MScRoot, ScNodesShowHide}
import io.suggest.sc.v.styl.ScCss
import io.suggest.sjs.common.empty.JsOptionUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.2020 14:41
  * Description: Контейнер для формы редактора узлов.
  */
class ScNodesR(
                lkNodesFormR        : LkNodesFormR,
                platformComponents  : PlatformComponents,
                platfromCss         : () => PlatformCssStatic,
                crCtxP              : React.Context[MCommonReactCtx],
                scCssP              : React.Context[ScCss],
              ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    circuitOptC                   : ReactConnectProxy[Option[LkNodesFormCircuit]],
                    isDiaFullScreenSomeC          : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private lazy val _onCloseClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ScNodesShowHide(visible = false) )
    }

    def render(s: State): VdomElement = {
      s.circuitOptC { circuitOptProxy =>
        val platCss = platfromCss()

        val circuitOpt = circuitOptProxy.value

        val diaChildren = List[VdomNode](

          // Заголовок
          platformComponents.diaTitle(Nil)(
            crCtxP.message( MsgCodes.`Nodes.management` ),
          ),

          // Содержимое диалога.
          MuiDialogContent()(
            circuitOpt.whenDefinedEl { circuit =>
              circuit.wrap(identity(_))( lkNodesFormR.component.apply )
            },
          ),

          // Кнопки внизу.
          MuiDialogActions(
            platformComponents.diaActionsProps()(platCss)
          )(
            MuiButton(
              new MuiButtonProps {
                override val size = MuiButtonSizes.large
                override val onClick = _onCloseClick
              }
            )(
              crCtxP.message( MsgCodes.`Close` )
            )
          ),
        )

        // Вложенный коннекшен допускается, т.к. обновление внешнего связано только с полным монтированием-демонтированием диалога.
        scCssP.consume { scCss =>
          s.isDiaFullScreenSomeC { isDiaFullScreenSomeProxy =>
            val _isFullScreen = isDiaFullScreenSomeProxy.value.value
            val _rootCssU = JsOptionUtil.maybeDefined( _isFullScreen )( scCss.Header.header.htmlClass )
            MuiDialog {
              val diaCss = new MuiDialogClasses {
                override val paper = platCss.Dialogs.paper.htmlClass
                // Если есть unsafe-зоны на экране, то нужен отступ сверху в fullscreen-режиме:
                override val root = _rootCssU
              }
              new MuiDialogProps {
                override val open = circuitOpt.nonEmpty
                override val classes = diaCss
                override val onClose = _onCloseClick
                override val fullScreen = _isFullScreen
              }
            } (
              diaChildren: _*
            )
          }
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        circuitOptC = propsProxy.connect( _.dialogs.nodes.circuit ),

        isDiaFullScreenSomeC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( mroot.dev.screen.info.isDialogWndFullScreen(450) )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
