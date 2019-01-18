package io.suggest.sc.v.dia

import chandu0101.scalajs.react.components.materialui.{MuiButton, MuiButtonProps, MuiButtonVariants, MuiColorTypes, MuiTypoGraphy}
import diode.UseValueEq
import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.dia.YesNoWz
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ReactEvent, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.19 18:52
  * Description: Контент окошечка с текстом об отказе юзера от .
  */
class WzInfoR {

  case class PropsVal(
                       message: String
                     )
    extends UseValueEq

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    private val _nextBtnClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB($, YesNoWz(false))
    }

    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value

      <.div(

        MuiTypoGraphy()(
          props.message
        ),

        MuiButton {
          new MuiButtonProps {
            override val color   = MuiColorTypes.primary
            override val variant = MuiButtonVariants.text
            override val onClick = _nextBtnClickCbF
          }
        } (
          Messages( MsgCodes.`Next` )
        )

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
