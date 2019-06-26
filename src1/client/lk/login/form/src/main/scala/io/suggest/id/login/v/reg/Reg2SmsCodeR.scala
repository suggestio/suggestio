package io.suggest.id.login.v.reg

import chandu0101.scalajs.react.components.materialui.{MuiFormGroup, MuiFormGroupProps}
import diode.react.ModelProxy
import io.suggest.id.login.m.reg.step2.MReg2SmsCode
import io.suggest.lk.r.sms.SmsCodeFormR
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 18:35
  * Description: Компонент формы ввода смс-кода.
  */
class Reg2SmsCodeR(
                    smsCodeFormR       : SmsCodeFormR,
                  ) {

  type Props = ModelProxy[MReg2SmsCode]


  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {
      MuiFormGroup(
        new MuiFormGroupProps {
          override val row = true
        }
      )(
        // Поле для ввода смс-кода:
        p.wrap { props =>
          for (smsCode <- props.smsCode) yield {
            smsCodeFormR.PropsVal(
              smsCode   = smsCode,
              disabled  = props.submitReq.isPending,
            )
          }
        }( smsCodeFormR.component.apply )(implicitly, OptFastEq.Wrapped(smsCodeFormR.SmsCodeFormRPropsValFastEq)),
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .renderBackend[Backend]
    .build

}
