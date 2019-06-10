package io.suggest.id.login.v.reg

import chandu0101.scalajs.react.components.materialui.{MuiFormGroup, MuiFormGroupProps}
import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.id.IdentConst
import io.suggest.id.login.m.reg.step0.MReg0Creds
import io.suggest.id.login.m.{RegEmailBlur, RegEmailEdit, RegPhoneBlur, RegPhoneEdit}
import io.suggest.id.login.v.epw.EpwTextFieldR
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 17:29
  * Description: Страница регистрации с вводом необходимых реквизитов.
  */
class Reg0CredsR(
                  epwTextFieldR      : EpwTextFieldR,
                ) {

  type Props = ModelProxy[MReg0Creds]

  class Backend( $: BackendScope[Props, Unit] ) {

    def render(p: Props): VdomElement = {
      MuiFormGroup(
        new MuiFormGroupProps {
          override val row = true
        }
      )(

        // Поле ввода email:
        {
          val regEmailBlurSome = Some( RegEmailBlur )
          p.wrap { props =>
            epwTextFieldR.PropsVal(
              state       = props.email,
              hasError    = false,
              mkAction    = RegEmailEdit,
              isPassword  = false,
              inputName   = IdentConst.Login.NAME_FN,
              label       = MsgCodes.`Your.email.addr`,
              onBlur      = regEmailBlurSome,
              placeHolder = MsgCodes.`Email.example`,
            )
          }(epwTextFieldR.apply)(implicitly, epwTextFieldR.EpwTextFieldPropsValFastEq)
        },


        // Поле ввода номера телефона:
        {
          val regPhoneBlurSome = Some( RegPhoneBlur )
          p.wrap { props =>
            epwTextFieldR.PropsVal(
              state       = props.phone,
              hasError    = false,
              mkAction    = RegPhoneEdit,
              isPassword  = false,
              inputName   = IdentConst.Login.PHONE_FN,
              label       = MsgCodes.`Mobile.phone.number`,
              placeHolder = MsgCodes.`Phone.number.example`,
              onBlur      = regPhoneBlurSome,
            )
          }(epwTextFieldR.apply)(implicitly, epwTextFieldR.EpwTextFieldPropsValFastEq)
        },
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .renderBackend[Backend]
    .build

}
