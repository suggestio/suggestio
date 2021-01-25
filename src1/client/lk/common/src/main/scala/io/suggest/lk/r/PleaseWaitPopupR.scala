package io.suggest.lk.r

import com.materialui.{MuiDialog, MuiDialogContent, MuiDialogProps, MuiDialogTitle, MuiLinearProgress, MuiLinearProgressProps, MuiProgressVariants}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import japgolly.scalajs.react.{BackendScope, ReactEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.PleaseWaitPopupCloseClick
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.sjs.common.empty.JsOptionUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 22:04
  * Description: Попап с просьбой подождать.
  */
object PleaseWaitPopupR {

  type Props = ModelProxy[Option[Long]]

  case class State(
                    isVisibleSomeC: ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private val _onClose = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, PleaseWaitPopupCloseClick )
    }

    def render(s: State): VdomElement = {
      lazy val _title = MuiDialogTitle()(
        Messages( MsgCodes.`Please.wait` ),
      )

      s.isVisibleSomeC { isVisibleProxy =>
        val _isVisible = isVisibleProxy.value.value
        println( _isVisible )
        MuiDialog(
          new MuiDialogProps {
            override val open = _isVisible
            override val hideBackdrop = true
            override val onClose = _onClose
          }
        ) (
          _title,
          MuiDialogContent()(
            MuiLinearProgress(
              new MuiLinearProgressProps {
                override val variant =
                  if (_isVisible) MuiProgressVariants.indeterminate
                  else MuiProgressVariants.determinate
                override val value = JsOptionUtil.maybeDefined( !_isVisible )( 0d )
              }
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
        isVisibleSomeC = propsProxy.connect { props =>
          println( props )
          OptionUtil.SomeBool( props.nonEmpty )
        },
      )
    }
    .renderBackend[Backend]
    .build

}
