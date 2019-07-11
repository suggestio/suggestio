package io.suggest.id.login.v.pwch

import com.materialui.{MuiFormGroup, MuiFormGroupProps}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.id.IdentConst
import io.suggest.id.login.m.pwch.MPwNew
import io.suggest.id.login.m.{NewPasswordBlur, SetPasswordEdit}
import io.suggest.id.login.v.stuff.TextFieldR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.19 19:18
  * Description: Ввод нового пароля в два поля.
  */
class PwNewR (
               textFieldR      : TextFieldR,
             ) {

  /**
    * @param isNew Ввод нового пароля?
    *              false - ввод первого пароля на регистрации.
    *              true - смена пароля на новый.
    */
  case class PropsVal(
                       pwNew      : MPwNew,
                       reqPending : Boolean,
                       isNew      : Boolean,
                     )
  implicit object PwNewRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.pwNew ===* b.pwNew) &&
      (a.reqPending ==* b.reqPending) &&
      (a.isNew ==* b.isNew)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      val setPasswordBlurSome = Some( NewPasswordBlur )

      MuiFormGroup(
        new MuiFormGroupProps {
          override val row = true
        }
      )(

        // Первое поле ввода пароля.
        propsProxy.wrap { p =>
          textFieldR.PropsVal(
            state       = p.pwNew.password1,
            hasError    = p.pwNew.isPasswordMismatchShown,
            mkAction    = Some( SetPasswordEdit(_: String, isRetype = false) ),
            isPassword  = true,
            inputName   = IdentConst.Login.PASSWORD_FN,    // По идее, вообще необязательно. По идее - "password"
            label       =
              if (p.isNew) MsgCodes.`Type.new.password`
              else MsgCodes.`Type.password`,
            placeHolder = "",
            onBlur      = setPasswordBlurSome,
            disabled    = p.reqPending,
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
            label       =
              if (p.isNew) MsgCodes.`Confirm.new.password`
              else MsgCodes.`Retype.password`,
            placeHolder = "",
            onBlur      = setPasswordBlurSome,
            disabled    = p.reqPending,
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
