package io.suggest.id.login.v

import chandu0101.scalajs.react.components.materialui.{Component_t, MuiButton, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiCheckBox, MuiCheckBoxProps, MuiFormControl, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps, MuiPaper, MuiTextField, MuiTextFieldProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.{BackendScope, Callback, PropsChildren, React, ReactEvent, ReactEventFromInput, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 16:49
  * Description: Форма входа по имени и паролю.
  */
class EpwFormR(
                commonReactCtxProv          : React.Context[MCommonReactCtx],
              ) {

  type Props_t = MEpwLoginS
  type Props = ModelProxy[Props_t]


  case class State(
                    nameC                   : ReactConnectProxy[String],
                    passwordC               : ReactConnectProxy[String],
                    loginBtnEnabledSomeC    : ReactConnectProxy[Some[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private def _onNameChange(event: ReactEventFromInput): Callback = {
      val name2 = event.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EpwSetName(name2) )
    }
    private val _onNameChangeCbF = ReactCommonUtil.cbFun1ToJsCb( _onNameChange )


    private def _onPasswordChange(event: ReactEventFromInput): Callback = {
      val password2 = event.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EpwSetPassword(password2) )
    }
    private val _onPasswordChangeCbF = ReactCommonUtil.cbFun1ToJsCb( _onPasswordChange )


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


    def render(s: State, children: PropsChildren): VdomElement = {
      val password = HtmlConstants.Input.password

      MuiPaper()(

        // Поддержка локализации:
        commonReactCtxProv.consume { crCtx =>

          /** Рендер одного input field'а для имени или пароля. */
          def _inputTextField(valueC: ReactConnectProxy[String], msgCode: String, inputName: String, onChangeCbF: js.Function1[ReactEventFromInput, Unit], isPassword: Boolean) = {
            valueC { valueProxy =>
              MuiTextField(
                new MuiTextFieldProps {
                  override val value = js.defined {
                    valueProxy.value
                  }
                  override val onChange = onChangeCbF
                  override val placeholder = crCtx.messages( msgCode )
                  override val name = inputName
                  override val `type` = if (isPassword) password else HtmlConstants.Input.text
                }
              )
            }
          }

          val loginBtnText = crCtx.messages( MsgCodes.`Login` ): VdomNode

          <.div(

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
                  _inputTextField( s.nameC, MsgCodes.`Username`, "email", _onNameChangeCbF, isPassword = false ),

                  // Поле ввода пароля:
                  _inputTextField( s.passwordC, MsgCodes.`Password`, password, _onPasswordChangeCbF, isPassword = true ),

                  children,

                  // Кнопка "Войти", аналог сабмита формы.
                  s.loginBtnEnabledSomeC { loginBtnEnabledSomeProxy =>
                    MuiButton(
                      new MuiButtonProps {
                        override val size     = MuiButtonSizes.large
                        override val variant  = MuiButtonVariants.contained
                        override val onClick  = _onLoginBtnClickCbF
                        override val disabled = !loginBtnEnabledSomeProxy.value.value
                        override val `type`   = HtmlConstants.Input.submit
                        override val fullWidth = true
                      }
                    )(
                      loginBtnText
                    )
                  }
                )
              )

            ),

          )

        },

      )
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        nameC                 = propsProxy.connect(_.name)( FastEq.AnyRefEq ),
        passwordC             = propsProxy.connect(_.password)( FastEq.AnyRefEq ),
        loginBtnEnabledSomeC  = propsProxy.connect(_.loginBtnEnabledSome)( FastEqUtil.RefValFastEq ),
      )
    }
    .renderBackendWithChildren[Backend]
    .build

  def apply(props: Props)(children: VdomNode*) = component( props )(children: _*)

}
