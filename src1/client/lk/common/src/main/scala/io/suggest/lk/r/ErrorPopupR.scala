package io.suggest.lk.r

import com.materialui.{MuiButton, MuiButtonProps, MuiDialog, MuiDialogActions, MuiDialogContent, MuiDialogProps, MuiDialogTitle}
import diode.react.ModelProxy
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.{ErrorPopupCloseClick, MErrorPopupS}
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 22:22
  * Description: Попап ошибки.
  */
final class ErrorPopupR {

  type Props_t = Option[MErrorPopupS]
  type Props = ModelProxy[Props_t]

  protected class Backend($: BackendScope[Props, Unit]) {

    private def closeBtnClick: Callback = {
      dispatchOnProxyScopeCB( $, ErrorPopupCloseClick )
    }
    private val _closeCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      closeBtnClick
    }

    def render(proxy: Props): VdomElement = {
      proxy().whenDefinedEl { popupS =>
        MuiDialog(
          new MuiDialogProps {
            override val open = popupS.exception.nonEmpty
            override val onClose = _closeCbF
          }
        )(
          MuiDialogTitle()(
            Messages( MsgCodes.`Something.gone.wrong` ),
          ),

          MuiDialogContent()(
            popupS.messages.headOption.whenDefinedEl { _ =>
              <.div(
                <.ul(
                  popupS.messages.iterator.zipWithIndex.toVdomArray { case (msg, i) =>
                    <.li(
                      ^.key := i.toString,
                      Messages( msg )
                    )
                  }
                ),
                <.br
              )
            },

            popupS.exception.whenDefinedEl { ex =>
              <.div(
                <.div(
                  ^.`class` := Css.Colors.RED,
                  Messages( MsgCodes.`Error` )
                ),

                <.div(
                  ex.toString()
                ),
                <.br,

                Messages( MsgCodes.`Please.try.again.later` )
              )
            },

          ),

          MuiDialogActions()(
            MuiButton(
              new MuiButtonProps {
                override val onClick = _closeCbF
              }
            )(
              Messages( MsgCodes.`Close` )
            )
          ),
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .stateless
    .renderBackend[Backend]
    .build

}
