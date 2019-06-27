package io.suggest.id.login.v.reg

import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.id.IdentConst
import io.suggest.id.login.m.reg.MRegS
import io.suggest.id.login.m.{SetPasswordBlur, SetPasswordEdit}
import io.suggest.id.login.v.stuff.TextFieldR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.19 14:03
  * Description: Форма сохранения пароля.
  */
class Reg4SetPasswordR(
                        textFieldR      : TextFieldR,
                      ) {

  type Props_t = MRegS
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      val setPasswordBlurSome = Some( SetPasswordBlur )

      <.div(

        // Имя пользователя (неизменяемое). Возможно, поможет для сохранения пароля в список паролей браузера.
        propsProxy.wrap { p =>
          textFieldR.PropsVal(
            state       = p.s0Creds.phone,
            hasError    = false,
            mkAction    = None,
            isPassword  = false,
            inputName   = IdentConst.Login.NAME_FN,    // По идее, вообще необязательно. По идее - "password"
            label       = MsgCodes.`Username`,
            placeHolder = "",
            onBlur      = None,
            disabled    = true,
            required    = false,
          )
        }( textFieldR.component.apply )( implicitly, textFieldR.EpwTextFieldPropsValFastEq ),

        // TODO Разделитель нужен сюда

        // Первое поле ввода пароля.
        propsProxy.wrap { p =>
          textFieldR.PropsVal(
            state       = p.s4SetPassword.password1,
            hasError    = false,
            mkAction    = Some( SetPasswordEdit(_: String, isRetype = false) ),
            isPassword  = true,
            inputName   = IdentConst.Login.PASSWORD_FN,    // По идее, вообще необязательно. По идее - "password"
            label       = MsgCodes.`Type.password`,
            placeHolder = "",
            onBlur      = setPasswordBlurSome,
            disabled    = p.s4SetPassword.submitReq.isPending,
          )
        }( textFieldR.component.apply )( implicitly, textFieldR.EpwTextFieldPropsValFastEq ),

        // Второе поле повторного ввода пароля.
        propsProxy.wrap { p =>
          textFieldR.PropsVal(
            state       = p.s4SetPassword.password2,
            hasError    = false,
            mkAction    = Some( SetPasswordEdit(_: String, isRetype = true) ),
            isPassword  = true,
            inputName   = "",
            label       = MsgCodes.`Retype.password`,
            placeHolder = "",
            onBlur      = setPasswordBlurSome,
            disabled    = p.s4SetPassword.submitReq.isPending,
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
