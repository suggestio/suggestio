package io.suggest.id.login.v.epw

import com.materialui.{MuiButton, MuiButtonClasses, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiFormControl, MuiFormControlLabel, MuiFormControlLabelProps, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps, MuiPaper}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.IdentConst
import io.suggest.id.login.m._
import io.suggest.id.login.m.epw.MEpwLoginS
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.v.stuff.{CheckBoxR, LoginProgressR, TextFieldR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
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
                textFieldR                  : TextFieldR,
                checkBoxR                   : CheckBoxR,
                loginProgressR              : LoginProgressR,
                commonReactCtxProv          : React.Context[MCommonReactCtx],
                loginFormCssCtx             : React.Context[LoginFormCss],
              ) {

  type Props_t = MEpwLoginS
  type Props = ModelProxy[Props_t]


  case class State(
                    loginReqPendingSomeC    : ReactConnectProxy[Some[Boolean]],
                    loginBtnDisabledSomeC   : ReactConnectProxy[Some[Boolean]],
                  )


  class Backend($: BackendScope[Props, State]) {

    /** Реакция на попытку сабмита формы мимо основной кнопки.
      * Возможно, enter с клавиатуры или ещё как-то.
      */
    private def _onFormSubmit(e: ReactEvent): Callback = {
      ReactCommonUtil.preventDefaultCB(e) >>
        ReactDiodeUtil.dispatchOnProxyScopeCB($, EpwDoLogin)
    }


    def render(propsProxy: Props, s: State): VdomElement = {
      MuiPaper()(

        // Поддержка локализации:
        <.form(
          ^.onSubmit ==> _onFormSubmit,

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
                textFieldR.PropsVal(
                  state       = props.name,
                  hasError    = props.loginReq.isFailed,
                  mkAction    = Some( EpwSetName.apply ),
                  isPassword  = false,
                  inputName   = IdentConst.Login.NAME_FN,
                  label       = MsgCodes.`Phone.or.email`,
                  placeHolder = MsgCodes.`Phone.number.example`,
                  disabled    = props.loginReq.isPending,
                )
              }( textFieldR.apply )( implicitly, textFieldR.EpwTextFieldPropsValFastEq ),

              // Поле ввода пароля:
              propsProxy.wrap { props =>
                textFieldR.PropsVal(
                  state       = props.password,
                  hasError    = props.loginReq.isFailed,
                  mkAction    = Some( EpwSetPassword.apply ),
                  isPassword  = true,
                  inputName   = IdentConst.Login.PASSWORD_FN,
                  label       = MsgCodes.`Password`,
                  disabled    = props.loginReq.isPending,
                  placeHolder = "",
                )
              }( textFieldR.apply )( implicitly, textFieldR.EpwTextFieldPropsValFastEq ),

              // Галочка "Чужой компьютер".
              MuiFormControlLabel {
                val labelText = commonReactCtxProv.consume { crCtx =>
                  crCtx.messages( MsgCodes.`Not.my.pc` )
                }
                val cbx = propsProxy.wrap { p =>
                  checkBoxR.PropsVal(
                    checked   = p.isForeignPc,
                    onChange  = EpwSetForeignPc,
                  )
                }( checkBoxR.apply )(implicitly, checkBoxR.CheckBoxRFastEq)
                new MuiFormControlLabelProps {
                  override val control = cbx.rawElement
                  override val label   = labelText.rawNode
                }
              },

              // Линейка ожидания:
              s.loginReqPendingSomeC { loginProgressR.component.apply },

              // Кнопка "Войти", аналог сабмита формы.
              {
                val loginBtnText = commonReactCtxProv.consume { crCtx =>
                  crCtx.messages( MsgCodes.`Login` )
                }
                loginFormCssCtx.consume { loginFormCss =>
                  val btnCss = new MuiButtonClasses {
                    override val root = loginFormCss.formControl.htmlClass
                  }
                  s.loginBtnDisabledSomeC { loginBtnDisabledSomeProxy =>
                    MuiButton(
                      new MuiButtonProps {
                        override val size       = MuiButtonSizes.large
                        override val variant    = MuiButtonVariants.contained
                        override val disabled   = loginBtnDisabledSomeProxy.value.value
                        override val fullWidth  = true
                        override val classes    = btnCss
                        override val `type`     = HtmlConstants.Input.submit
                      }
                    )(
                      loginBtnText
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

        loginReqPendingSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.loginReq.isPending )
        }( FastEq.AnyRefEq ),

        loginBtnDisabledSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( !props.loginBtnEnabled )
        },

      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component( props )

}
