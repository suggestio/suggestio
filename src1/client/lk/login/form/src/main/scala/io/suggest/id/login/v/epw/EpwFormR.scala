package io.suggest.id.login.v.epw

import chandu0101.scalajs.react.components.materialui.{MuiButton, MuiButtonClasses, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiFormControl, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps, MuiPaper}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m._
import io.suggest.id.login.m.epw.MEpwLoginS
import io.suggest.id.login.v.LoginFormCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ScalaComponent}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 16:49
  * Description: Форма входа по имени и паролю.
  */
class EpwFormR(
                epwTextFieldR               : EpwTextFieldR,
                foreignPcCheckBoxR          : ForeignPcCheckBoxR,
                loginProgressR              : LoginProgressR,
                commonReactCtxProv          : React.Context[MCommonReactCtx],
                loginFormCssCtx             : React.Context[LoginFormCss],
              ) {

  type Props_t = MEpwLoginS
  type Props = ModelProxy[Props_t]


  case class State(
                    loginBtnEnabledSomeC    : ReactConnectProxy[Some[Boolean]],
                    loginReqPendingSomeC    : ReactConnectProxy[Some[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private def _onLoginBtnClick(event: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EpwDoLogin )
    private val _onLoginBtnClickCbF = ReactCommonUtil.cbFun1ToJsCb( _onLoginBtnClick )


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
                  inputName   = "email",
                  msgCode     = MsgCodes.`Username`
                )
              }( epwTextFieldR.apply )( implicitly, epwTextFieldR.EpwTextFieldPropsValFastEq ),

              // Поле ввода пароля:
              propsProxy.wrap { props =>
                epwTextFieldR.PropsVal(
                  state       = props.password,
                  hasError    = props.loginReq.isFailed,
                  mkAction    = EpwSetPassword,
                  isPassword  = true,
                  inputName   = HtmlConstants.Input.password,
                  msgCode     = MsgCodes.`Password`
                )
              }( epwTextFieldR.apply )( implicitly, epwTextFieldR.EpwTextFieldPropsValFastEq ),

              // Галочка "Чужой компьютер".
              propsProxy.wrap(_.isForeignPcSome)( foreignPcCheckBoxR.apply )(implicitly, FastEqUtil.RefValFastEq),

              // Линейка ожидания:
              s.loginReqPendingSomeC { loginProgressR.component.apply },

              // Кнопка "Войти", аналог сабмита формы.
              {
                val loginText = commonReactCtxProv.consume { crCtx =>
                  crCtx.messages( MsgCodes.`Login` )
                }
                s.loginBtnEnabledSomeC { loginBtnEnabledSomeProxy =>
                  loginFormCssCtx.consume { loginFormCss =>
                    val btnCss = new MuiButtonClasses {
                      override val root = loginFormCss.epwFormControl.htmlClass
                    }
                    MuiButton(
                      new MuiButtonProps {
                        override val size     = MuiButtonSizes.large
                        override val variant  = MuiButtonVariants.contained
                        override val onClick  = _onLoginBtnClickCbF
                        override val disabled = !loginBtnEnabledSomeProxy.value.value
                        override val fullWidth = true
                        override val classes  = btnCss
                      }
                    )(
                      loginText
                    )
                  }
                }
              },

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
        loginBtnEnabledSomeC  = propsProxy.connect(_.loginBtnEnabledSome)( FastEqUtil.RefValFastEq ),
        loginReqPendingSomeC  = propsProxy.connect(_.isShowPendingSome)( FastEqUtil.RefValFastEq ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
