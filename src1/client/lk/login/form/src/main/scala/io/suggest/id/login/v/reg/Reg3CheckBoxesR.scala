package io.suggest.id.login.v.reg

import chandu0101.scalajs.react.components.materialui.{MuiFormGroup, MuiFormGroupProps}
import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.id.login.m.{RegPdnSetAccepted, RegTosSetAccepted}
import io.suggest.id.login.m.reg.step3.MReg3CheckBoxes
import io.suggest.id.login.v.stuff.CheckBoxR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 18:52
  * Description: Шаг финальных галочек и окончания регистрации.
  */
class Reg3CheckBoxesR(
                       checkBoxR: CheckBoxR
                     ) {

  type Props = ModelProxy[MReg3CheckBoxes]


  class Backend( $: BackendScope[Props, Unit] ) {

    def render(p: Props): VdomElement = {
      MuiFormGroup(
        new MuiFormGroupProps {
          override val row = true
        }
      )(

        // Галочка согласия с офертой suggest.io:
        p.wrap { props =>
          checkBoxR.PropsVal(
            checked   = props.tos.isChecked,
            msgCode   = MsgCodes.`I.accept.terms.of.service`,
            onChange  = RegTosSetAccepted,
          )
        }(checkBoxR.apply)(implicitly, checkBoxR.CheckBoxRFastEq),

        // Галочка разрешения на обработку ПДн:
        p.wrap { props =>
          checkBoxR.PropsVal(
            checked   = props.pdn.isChecked,
            msgCode   = MsgCodes.`I.allow.personal.data.processing`,
            onChange  = RegPdnSetAccepted,
          )
        }(checkBoxR.apply)(implicitly, checkBoxR.CheckBoxRFastEq),

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .renderBackend[Backend]
    .build

}
