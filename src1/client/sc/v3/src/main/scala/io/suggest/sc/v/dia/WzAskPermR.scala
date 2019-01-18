package io.suggest.sc.v.dia

import chandu0101.scalajs.react.components.materialui.{MuiButton, MuiButtonProps, MuiButtonVariants, MuiColorTypes, MuiPaper, MuiTypoGraphy}
import diode.UseValueEq
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.dia.YesNoWz
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ReactEvent, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 17:48
  * Description: Контент для вопроса в мастере настройки.
  */
class WzAskPermR {

  case class PropsVal(
                       message: String,
                     )
    extends UseValueEq

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    private def _yesNoCbF(yesNo: Boolean) = {
      ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
        ReactDiodeUtil.dispatchOnProxyScopeCB($, YesNoWz(yesNo))
      }
    }

    /** Callback клика по кнопке разрешения. */
    private val _allowClickCbF = _yesNoCbF(true)

    /** Callback нажатия на кнопку отказа. */
    private val _laterClickCbF = _yesNoCbF(false)


    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value
      <.div(

        // Плашка с вопросом геолокации.
        MuiPaper()(
          MuiTypoGraphy()(
            props.message
          )
        ),

        // Кнопка "Позже"
        MuiButton {
          new MuiButtonProps {
            override val color   = MuiColorTypes.secondary
            override val variant = MuiButtonVariants.text
            override val onClick = _allowClickCbF
          }
        } (
          Messages( MsgCodes.`Later` )
        ),

        // Кнопка "Разрешить"
        MuiButton {
          new MuiButtonProps {
            override val color   = MuiColorTypes.primary
            override val variant = MuiButtonVariants.contained
            override val onClick = _laterClickCbF
          }
        } (
          Messages( MsgCodes.`Allow.0`, MsgCodes.`GPS` ),
          HtmlConstants.SPACE,
          HtmlConstants.GREATER,
        )

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsProxy: Props ) = component( propsProxy )

}
