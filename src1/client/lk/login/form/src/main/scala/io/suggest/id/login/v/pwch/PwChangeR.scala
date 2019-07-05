package io.suggest.id.login.v.pwch

import com.materialui.{MuiFormControl, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps}
import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.id.IdentConst
import io.suggest.id.login.m.MLoginRootS
import io.suggest.id.login.v.stuff.TextFieldR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.19 19:10
  * Description: Компонент с формой смены пароля.
  */
class PwChangeR (
                  textFieldR  : TextFieldR,
                  pwNewR      : PwNewR,
                ) {

  // Зависим от корневой модели, чтобы иметь доступ к пошаренному инстансу pwNew.
  type Props_t = MLoginRootS
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {
      <.form(

        MuiFormControl(
          new MuiFormControlProps {
            override val component = js.defined( <.fieldset.name )
          }
        )(

          MuiFormGroup(
            new MuiFormGroupProps {
              override val row = true
            }
          )(

            p.wrap { mroot =>
              textFieldR.PropsVal(
                state       = mroot.pwCh.pwOld,
                hasError    = false,
                mkAction    = None, // TODO
                isPassword  = true,
                inputName   = IdentConst.Login.PASSWORD_FN,
                label       = MsgCodes.`Type.current.password`,
                placeHolder = "",
                onBlur      = None, // TODO,
                disabled    = mroot.pwCh.submitReq.isPending,
              )
            }(textFieldR.apply)(implicitly, textFieldR.EpwTextFieldPropsValFastEq),

          ),

          p.wrap { mroot =>
            pwNewR.PropsVal(
              pwNew       = mroot.overall.pwNew,
              reqPending  = mroot.pwCh.submitReq.isPending,
            )
          }(pwNewR.component.apply)(implicitly, pwNewR.PwNewRPropsValFastEq),

          // TODO Кнопка сабмита.

        )

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
