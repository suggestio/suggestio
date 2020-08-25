package io.suggest.sc.v.dia.nodes

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.LkNodesFormCircuit
import io.suggest.lk.nodes.form.r.LkNodesFormR
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.m.ScNodesShowHide
import io.suggest.sc.m.dia.MScNodes
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
              ) {

  type Props_t = MScNodes
  type Props = ModelProxy[Props_t]

  case class State(
                    circuitOptC    : ReactConnectProxy[Option[LkNodesFormCircuit]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private lazy val _onCloseClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ScNodesShowHide(visible = false) )
    }

    def render(s: State): VdomElement = {
      s.circuitOptC { circuitOptProxy =>
        val platCss = platfromCss()

        val circuitOpt = circuitOptProxy.value
        MuiDialog {
          val diaCss = new MuiDialogClasses {
            override val paper = platCss.Dialogs.paper.htmlClass
          }
          new MuiDialogProps {
            override val open = circuitOpt.nonEmpty
            override val classes = diaCss
            override val onClose = _onCloseClick
            // TODO override val fullScreen = true на узких экранах
          }
        } (
          platformComponents.diaTitle(Nil)(
            crCtxP.message( MsgCodes.`Nodes.management` ),
          ),

          // Содержимое диалога.
          MuiDialogContent()(
            circuitOpt.whenDefinedEl { circuit =>
              circuit.wrap(identity(_))( lkNodesFormR.component.apply )
            },
          ),

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
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        circuitOptC = propsProxy.connect( _.circuit ),

      )
    }
    .renderBackend[Backend]
    .build

}
