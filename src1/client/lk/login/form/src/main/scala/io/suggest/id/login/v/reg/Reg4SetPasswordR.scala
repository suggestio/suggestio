package io.suggest.id.login.v.reg

import com.materialui.{MuiFormGroup, MuiFormGroupProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.MsgCodes
import io.suggest.id.IdentConst
import io.suggest.id.login.m.MLoginRootS
import io.suggest.id.login.v.pwch.PwNewR
import io.suggest.id.login.v.stuff.{ErrorSnackR, TextFieldR}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.spa.DiodeUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.19 14:03
  * Description: Форма сохранения пароля.
  */
class Reg4SetPasswordR(
                        pwNewR          : PwNewR,
                        errorSnackR     : ErrorSnackR,
                        textFieldR      : TextFieldR,
                      ) {

  type Props_t = MLoginRootS
  type Props = ModelProxy[Props_t]

  case class State(
                    submitReqExC      : ReactConnectProxy[Throwable],
                  )

  class Backend($: BackendScope[Props, State]) {

    def render(propsProxy: Props, s: State): VdomElement = {
      <.div(

        MuiFormGroup(
          new MuiFormGroupProps {
            override val row = true
          }
        )(
          
          // Имя пользователя (неизменяемое). Возможно, поможет для сохранения пароля в список паролей браузера.
          propsProxy.wrap { p =>
            textFieldR.PropsVal(
              state       = p.reg.s0Creds.phone,
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

        ),

        propsProxy.wrap { mroot =>
          pwNewR.PropsVal(
            pwNew       = mroot.overall.pwNew,
            reqPending  = mroot.reg.s4SetPassword.submitReq.isPending,
            isNew       = false,
          )
        }(pwNewR.component.apply)(implicitly, pwNewR.PwNewRPropsValFastEq),

        s.submitReqExC { errorSnackR.component.apply },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        submitReqExC = propsProxy.connect(_.reg.s4SetPassword.submitReq.exceptionOrNull)( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

}
