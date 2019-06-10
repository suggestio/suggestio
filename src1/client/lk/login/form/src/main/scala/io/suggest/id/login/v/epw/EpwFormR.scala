package io.suggest.id.login.v.epw

import chandu0101.scalajs.react.components.materialui.{MuiFormControl, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps, MuiPaper}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.IdentConst
import io.suggest.id.login.m._
import io.suggest.id.login.m.epw.MEpwLoginS
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.v.stuff.{ButtonR, CheckBoxR, LoginProgressR}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, React, ScalaComponent}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 16:49
  * Description: Форма входа по имени и паролю.
  */
class EpwFormR(
                epwTextFieldR               : EpwTextFieldR,
                checkBoxR                   : CheckBoxR,
                buttonR                     : ButtonR,
                loginProgressR              : LoginProgressR,
                commonReactCtxProv          : React.Context[MCommonReactCtx],
                loginFormCssCtx             : React.Context[LoginFormCss],
              ) {

  type Props_t = MEpwLoginS
  type Props = ModelProxy[Props_t]


  case class State(
                    loginReqPendingSomeC    : ReactConnectProxy[Some[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    /** Реакция на попытку сабмита формы мимо основной кнопки.
      * Возможно, enter с клавиатуры или ещё как-то.
      */
    private val _onFormSubmit: Callback = {
      $.props >>= { propsProxy: Props =>
        if (propsProxy.value.loginBtnEnabled) {
          propsProxy.dispatchCB( EpwDoLogin )
        } else {
          Callback.empty
        }
      }
    }


    def render(propsProxy: Props, s: State): VdomElement = {
      MuiPaper()(

        // Поддержка локализации:
        <.form(
          ^.onSubmit --> _onFormSubmit,

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
              // Поле имени пользователя:
              propsProxy.wrap { props =>
                epwTextFieldR.PropsVal(
                  state       = props.name,
                  hasError    = props.loginReq.isFailed,
                  mkAction    = EpwSetName,
                  isPassword  = false,
                  inputName   = IdentConst.Login.NAME_FN,
                  label       = MsgCodes.`Phone.or.email`,
                  placeHolder = MsgCodes.`Phone.number.example`,
                  disabled    = props.loginReq.isPending,
                )
              }( epwTextFieldR.apply )( implicitly, epwTextFieldR.EpwTextFieldPropsValFastEq ),

              // Поле ввода пароля:
              propsProxy.wrap { props =>
                epwTextFieldR.PropsVal(
                  state       = props.password,
                  hasError    = props.loginReq.isFailed,
                  mkAction    = EpwSetPassword,
                  isPassword  = true,
                  inputName   = IdentConst.Login.PASSWORD_FN,
                  label       = MsgCodes.`Password`,
                  disabled    = props.loginReq.isPending,
                  placeHolder = "",
                )
              }( epwTextFieldR.apply )( implicitly, epwTextFieldR.EpwTextFieldPropsValFastEq ),

              // Галочка "Чужой компьютер".
              propsProxy.wrap { p =>
                checkBoxR.PropsVal(
                  checked   = p.isForeignPc,
                  msgCode   = MsgCodes.`Not.my.pc`,
                  onChange  = EpwSetForeignPc,
                )
              }( checkBoxR.apply )(implicitly, checkBoxR.CheckBoxRFastEq),

              // Линейка ожидания:
              s.loginReqPendingSomeC { loginProgressR.component.apply },

              // Кнопка "Войти", аналог сабмита формы.
              propsProxy.wrap { p =>
                buttonR.PropsVal(
                  disabled = !p.loginBtnEnabled,
                  onClick  = EpwDoLogin,
                  msgCode  = MsgCodes.`Login`,
                )
              }( buttonR.apply )(implicitly, buttonR.ButtonRPropsValFastEq),

            )
          )

        ),

      )
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        loginReqPendingSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.loginReq.isPending )
        }( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
