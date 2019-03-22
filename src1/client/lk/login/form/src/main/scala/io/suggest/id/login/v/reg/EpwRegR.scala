package io.suggest.id.login.v.reg

import chandu0101.scalajs.react.components.materialui.{MuiFormControl, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps, MuiPaper}
import diode.react.ModelProxy
import io.suggest.captcha.MCaptchaCookiePaths
import io.suggest.i18n.MsgCodes
import io.suggest.id.IdentConst
import io.suggest.id.login.m.{EpwRegSubmit, RegEmailBlur, RegEmailEdit}
import io.suggest.id.login.m.reg.MEpwRegS
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.v.epw.EpwTextFieldR
import io.suggest.id.login.v.stuff.ButtonR
import io.suggest.lk.r.captcha.CaptchaFormR
import io.suggest.react.ReactDiodeUtil
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
               loginFormCssCtx    : React.Context[LoginFormCss],
             ) {

  type Props_t = MEpwRegS
  type Props = ModelProxy[Props_t]


  case class State(
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private def _onFormSubmit: Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, EpwRegSubmit )

    def render(p: Props, s: State): VdomElement = {
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
                  )
                }( captchaFormR.apply )(implicitly, captchaFormR.CaptchaFormRPropsValFastEq)

                loginFormCssCtx.consume { loginFormCss =>
                  <.div(
                    loginFormCss.formControl,
                    captcha
                  )
                }
              },

              // Кнопка регистрации.
              p.wrap { props =>
                buttonR.PropsVal(
                  disabled =
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
        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
      )
    }
    .renderBackend[Backend]
    .build


  def apply( propsProxy: Props ) = component( propsProxy )

}
