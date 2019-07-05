package io.suggest.id.login.v.pwch

import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.id.IdentConst
import io.suggest.id.login.m.pwch.IPwNewSubmit
import io.suggest.id.login.m.{SetPasswordBlur, SetPasswordEdit}
import io.suggest.id.login.v.stuff.TextFieldR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.19 19:18
  * Description: Ввод нового пароля в два поля.
  */
class PwNewR (
               textFieldR      : TextFieldR,
             ) {

  type Props_t = IPwNewSubmit
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      val setPasswordBlurSome = Some( SetPasswordBlur )

      <.div(

        // Первое поле ввода пароля.
        propsProxy.wrap { p =>
          textFieldR.PropsVal(
            state       = p.pwNew.password1,
            hasError    = p.pwNew.isPasswordMismatchShown,
            mkAction    = Some( SetPasswordEdit(_: String, isRetype = false) ),
            isPassword  = true,
            inputName   = IdentConst.Login.PASSWORD_FN,    // По идее, вообще необязательно. По идее - "password"
            label       = MsgCodes.`Type.password`,
            placeHolder = "",
            onBlur      = setPasswordBlurSome,
            disabled    = p.submitReq.isPending,
          )
        }( textFieldR.component.apply )( implicitly, textFieldR.EpwTextFieldPropsValFastEq ),

        // Второе поле повторного ввода пароля.
        propsProxy.wrap { p =>
          textFieldR.PropsVal(
            state       = p.pwNew.password2,
            hasError    = p.pwNew.isPasswordMismatchShown,
            mkAction    = Some( SetPasswordEdit(_: String, isRetype = true) ),
            isPassword  = true,
            inputName   = "",
            label       = MsgCodes.`Retype.password`,
            placeHolder = "",
            onBlur      = setPasswordBlurSome,
            disabled    = p.submitReq.isPending,
          )
        }( textFieldR.component.apply )( implicitly, textFieldR.EpwTextFieldPropsValFastEq ),

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
