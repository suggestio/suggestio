package io.suggest.id.login.v

import chandu0101.scalajs.react.components.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiPaper, MuiTextField, MuiTextFieldProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.{EpwDoLogin, EpwSetName, EpwSetPassword, MEpwLoginS}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ReactEventFromInput, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 16:49
  * Description: Форма входа по имени и паролю.
  */
class EpwFormR(
                commonReactCtxProv        : React.Context[MCommonReactCtx],
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


    def render(s: State): VdomElement = {
      MuiPaper()(

        // Поддержка локализации:
        commonReactCtxProv.consume { crCtx =>

          /** Рендер одного input field'а для имени или пароля. */
          def _inputTextField(valueC: ReactConnectProxy[String], msgCode: String, onChangeCbF: js.Function1[ReactEventFromInput, Unit]) = {
            valueC { valueProxy =>
              MuiTextField(
                new MuiTextFieldProps {
                  override val value = js.defined {
                    valueProxy.value
                  }
                  override val onChange = onChangeCbF
                  override val placeholder = crCtx.messages( msgCode )
                }
              )
            }
          }

          val loginBtnText = crCtx.messages( MsgCodes.`Login` ): VdomNode

          <.div(

            // Поле имени пользователя:
            _inputTextField( s.nameC, MsgCodes.`Name`, _onNameChangeCbF ),

            // Поле ввода пароля:
            _inputTextField( s.passwordC, MsgCodes.`Password`, _onPasswordChangeCbF ),

            // Кнопка "Войти":
            s.loginBtnEnabledSomeC { loginBtnEnabledSomeProxy =>
              MuiButton(
                new MuiButtonProps {
                  override val size     = MuiButtonSizes.large
                  override val variant  = MuiButtonVariants.contained
                  override val onClick  = _onLoginBtnClickCbF
                  override val disabled = !loginBtnEnabledSomeProxy.value.value
                }
              )(
                loginBtnText
              )
            },

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
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
