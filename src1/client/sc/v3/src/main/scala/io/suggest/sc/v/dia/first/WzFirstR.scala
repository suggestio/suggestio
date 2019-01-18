package io.suggest.sc.v.dia.first

import chandu0101.scalajs.react.components.materialui.{MuiDialog, MuiDialogProps}
import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.m.dia.first.{MWzFirstS, MWzFrames, MWzQuestions}
import io.suggest.sc.v.dia.{WzAskPermR, WzInfoR}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 12:37
  * Description: React-компонент для первого запуска система.
  */
class WzFirstR(
                wzAskFrameR    : WzAskPermR,
                wzInfoFrameR   : WzInfoR,
              ) {

  type Props_t = Option[MWzFirstS]
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsOptProxy: Props): VdomElement = {
      val propsOpt = propsOptProxy.value

      MuiDialog {
        new MuiDialogProps {
          override val open = propsOpt.fold(false)(_.visible)
          override val disableBackdropClick = true
          override val disableEscapeKeyDown = true
          override val fullScreen = true
        }
      } (
        propsOpt.whenDefinedEl { props =>
          props.frame match {

            // Рендер вопроса
            case MWzFrames.AskPerm =>
              val msgCodeOpt = props.question match {
                case MWzQuestions.GeoLocPerm =>
                  Some( MsgCodes.`0.uses.geoloc.to.find.ads` )
                case MWzQuestions.BlueToothPerm =>
                  Some( MsgCodes.`0.uses.bt.to.find.ads.indoor` )
                case _ =>
                  None
              }
              msgCodeOpt.whenDefinedEl { msgCode =>
                val pv = wzAskFrameR.PropsVal(
                  message = Messages( msgCode, MsgCodes.`Suggest.io` )
                )
                propsOptProxy.wrap(_ => pv)( wzAskFrameR.apply )
              }

            // Рендер инфы
            case MWzFrames.Info =>
              val compNameEith = props.question match {
                case MWzQuestions.GeoLocPerm =>
                  Right( MsgCodes.`GPS` )
                case MWzQuestions.BlueToothPerm =>
                  Right( MsgCodes.`Bluetooth` )
                case MWzQuestions.Finish =>
                  Left( MsgCodes.`Settings.done.0.ready.for.using` )
              }
              val pv = wzInfoFrameR.PropsVal(
                message = compNameEith.fold(
                  msgCode  => Messages( msgCode, MsgCodes.`Suggest.io` ),
                  compName => Messages( MsgCodes.`You.can.enable.0.later.on.left.panel`, compName )
                )
              )
              propsOptProxy.wrap(_ => pv)( wzInfoFrameR.apply )

          }
        }
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProps: Props) = component( propsOptProps )

}
