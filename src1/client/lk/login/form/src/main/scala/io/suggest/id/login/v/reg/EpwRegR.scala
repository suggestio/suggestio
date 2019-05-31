package io.suggest.id.login.v.reg

import chandu0101.scalajs.react.components.materialui.{MuiFormControl, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps, MuiPaper}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.captcha.MCaptchaCookiePaths
import io.suggest.i18n.MsgCodes
import io.suggest.id.IdentConst
import io.suggest.id.login.m.{EpwRegSubmit, RegEmailBlur, RegEmailEdit}
import io.suggest.id.login.m.reg.MEpwRegS
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.v.epw.EpwTextFieldR
import io.suggest.id.login.v.stuff.{ButtonR, LoginProgressR}
import io.suggest.lk.r.captcha.CaptchaFormR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.{BackendScope, Callback, React, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.19 16:22
  * Description: Форма начала регистрации по имени-паролю.
  */
class EpwRegR(
               epwTextFieldR      : EpwTextFieldR,
               buttonR            : ButtonR,
               captchaFormR       : CaptchaFormR,
               loginProgressR     : LoginProgressR,
               loginFormCssCtx    : React.Context[LoginFormCss],
             ) {

  type Props_t = MEpwRegS
  type Props = ModelProxy[Props_t]


  case class State(
                    isSubmitPendingSomeC: ReactConnectProxy[Some[Boolean]]
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private val _onFormSubmit: Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EpwRegSubmit )

    def render(p: Props, s: State): VdomElement = {
      val formContent = MuiFormControl(
        new MuiFormControlProps {
          override val component = js.defined( <.fieldset.name )
        }
      )(
        MuiFormGroup(
          new MuiFormGroupProps {
            override val row = true
          }
        )(

          // Поле ввода email:
          {
            val regEmailBlurSome = Some( RegEmailBlur )
            p.wrap { props =>
              epwTextFieldR.PropsVal(
                state       = props.email,
                hasError    = false,
                mkAction    = RegEmailEdit,
                isPassword  = false,
                inputName   = IdentConst.Login.NAME_FN,
                msgCode     = MsgCodes.`Your.email.addr`,
                onBlur      = regEmailBlurSome,
                disabled    = props.submitReq.isPending,
              )
            }(epwTextFieldR.apply)(implicitly, epwTextFieldR.EpwTextFieldPropsValFastEq)
          },

          // Капча - пока простая и уже рабочая собственная капча, для ускорения разработки.
          // Правда, в эпоху нейросетей она не защищает ни от чего кроме наиболее примитивных угроз.
          {
            val captcha = p.wrap { props =>
              captchaFormR.PropsVal(
                captcha     = props.captcha,
                cookiePath  = MCaptchaCookiePaths.EpwReg,
                disabled    = props.submitReq.isPending,
              )
            }( captchaFormR.apply )(implicitly, captchaFormR.CaptchaFormRPropsValFastEq)

            loginFormCssCtx.consume { loginFormCss =>
              <.div(
                loginFormCss.formControl,
                captcha
              )
            }
          },

          // Прогресс-бар ожидания...
          s.isSubmitPendingSomeC { loginProgressR.component.apply },

          // Кнопка регистрации.
          p.wrap { props =>
            buttonR.PropsVal(
              disabled =
                props.submitReq.isPending ||
                !props.captcha.typed.isValid ||
                !props.email.isValid ||
                props.captcha.typed.value.isEmpty ||
                props.email.value.isEmpty,
              onClick  = EpwRegSubmit,
              msgCode  = MsgCodes.`Sign.up`,
            )
          }(buttonR.apply)(implicitly, buttonR.ButtonRPropsValFastEq)

        )
      )

      MuiPaper()(
        s.isSubmitPendingSomeC { isFormDisabledSomeProxy =>
          <.form(
            ReactCommonUtil.maybe( isFormDisabledSomeProxy.value.value ) {
              TagMod(
                ^.onSubmit --> _onFormSubmit,
                ^.disabled  := true,
              )
            },
            formContent
          )
        }
      )

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isSubmitPendingSomeC = propsProxy.connect(_.isSubmitReqPendingSome)( FastEqUtil.RefValFastEq )
      )
    }
    .renderBackend[Backend]
    .build


  def apply( propsProxy: Props ) = component( propsProxy )

}
