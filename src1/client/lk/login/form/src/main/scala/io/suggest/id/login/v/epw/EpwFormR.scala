package io.suggest.id.login.v.epw

import com.materialui.{MuiButton, MuiButtonClasses, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiColorTypes, MuiFormControl, MuiFormControlLabel, MuiFormControlLabelProps, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps, MuiLink, MuiLinkClasses, MuiLinkProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.IdentConst
import io.suggest.id.login.MLoginTabs
import io.suggest.id.login.m._
import io.suggest.id.login.m.epw.MEpwLoginS
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.v.stuff.{CheckBoxR, LoginProgressR, TextFieldR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.SioPages
import japgolly.scalajs.react.extra.router.RouterCtl
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
                crCtxProv                   : React.Context[MCommonReactCtx],
                loginFormCssCtx             : React.Context[LoginFormCss],
                routerCtlRctx               : React.Context[RouterCtl[SioPages.Login]],
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

    private def _onPwRecoverLinkClick(e: ReactEvent): Callback = {
      ReactCommonUtil.preventDefaultCB(e) >>
        ReactDiodeUtil.dispatchOnProxyScopeCB($, PwReset(true) )
    }
    private val _onPwRecoverLinkClickCbF = ReactCommonUtil.cbFun1ToJsCb( _onPwRecoverLinkClick )


    def render(propsProxy: Props, s: State): VdomElement = {
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
            {
              val mkActionSome = Some( EpwSetName.apply _ )
              propsProxy.wrap { props =>
                textFieldR.PropsVal(
                  state       = props.name,
                  hasError    = props.loginReq.isFailed,
                  mkAction    = mkActionSome,
                  isPassword  = false,
                  inputName   = IdentConst.Login.NAME_FN,
                  label       = MsgCodes.`Phone.or.email`,
                  placeHolder = MsgCodes.`Phone.number.example`,
                  disabled    = props.loginReq.isPending,
                )
              }( textFieldR.apply )( implicitly, textFieldR.EpwTextFieldPropsValFastEq )
            },

            // Поле ввода пароля:
            {
              val mkActionSome = Some( SetPassword.apply _ )
              propsProxy.wrap { props =>
                textFieldR.PropsVal(
                  state       = props.password,
                  hasError    = props.loginReq.isFailed,
                  mkAction    = mkActionSome,
                  isPassword  = true,
                  inputName   = IdentConst.Login.PASSWORD_FN,
                  label       = MsgCodes.`Password`,
                  disabled    = props.loginReq.isPending,
                  placeHolder = "",
                )
              }( textFieldR.apply )( implicitly, textFieldR.EpwTextFieldPropsValFastEq )
            },

            // Галочка "Чужой компьютер".
            MuiFormControlLabel {
              val labelText = crCtxProv.message( MsgCodes.`Not.my.pc` )

              val cbx = propsProxy.wrap { p =>
                checkBoxR.PropsVal(
                  checked   = p.isForeignPc,
                  onChange  = EpwSetForeignPc,
                  disabled  = p.loginReq.isPending,
                )
              }( checkBoxR.apply )(implicitly, checkBoxR.CheckBoxRFastEq)
              new MuiFormControlLabelProps {
                override val control = cbx.rawElement
                override val label   = labelText.rawNode
              }
            },

            // Ссылка "Забыл пароль":
            loginFormCssCtx.consume { loginFormCssCtx =>
              val css = new MuiLinkClasses {
                override val root = loginFormCssCtx.forgotPassword.htmlClass
              }
              routerCtlRctx.consume { routerCtl =>
                MuiLink {
                  new MuiLinkProps {
                    override val color = MuiColorTypes.textPrimary
                    val href = routerCtl.urlFor( SioPages.Login( MLoginTabs.EpwLogin ) ).value
                    override val onClick = _onPwRecoverLinkClickCbF
                    override val classes = css
                  }
                } (
                  crCtxProv.message( MsgCodes.`Forgot.password` )
                )
              }
            },

            // Линейка ожидания:
            s.loginReqPendingSomeC { loginProgressR.component.apply },

            // Кнопка "Войти", аналог сабмита формы.
            {
              val loginBtnText = crCtxProv.message( MsgCodes.`Login` )

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

          ),
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

}
