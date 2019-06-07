package io.suggest.id.login.v.reg

import chandu0101.scalajs.react.components.materialui.{MuiFormGroup, MuiFormGroupProps, MuiStepContent}
import diode.react.ModelProxy
import io.suggest.id.login.m.reg.step1.MReg1Captcha
import io.suggest.id.login.v.LoginFormCss
import io.suggest.lk.r.captcha.CaptchaFormR
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 17:41
  * Description: Шаг1 - ввод и проверка капчи.
  */
class Reg1CaptchaR(
                    captchaFormR       : CaptchaFormR,
                    loginFormCssCtx    : React.Context[LoginFormCss],
                  ) {

  type Props = ModelProxy[MReg1Captcha]

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomElement = {
      MuiStepContent()(
        MuiFormGroup(
          new MuiFormGroupProps {
            override val row = true
          }
        )(

          // Капча - пока простая и уже рабочая собственная капча, для ускорения разработки.
          // Правда, в эпоху нейросетей она не защищает ни от чего кроме наиболее примитивных угроз.
          {
            val captcha = p.wrap { props =>
              for (captcha <- props.captcha) yield {
                CaptchaFormR.PropsVal(
                  captcha     = captcha,
                  disabled    = props.submitReq.isPending,
                )
              }
            }( captchaFormR.apply )(implicitly, OptFastEq.Wrapped(CaptchaFormR.CaptchaFormRPropsValFastEq))

            loginFormCssCtx.consume { loginFormCss =>
              <.div(
                loginFormCss.formControl,
                captcha
              )
            }
          },

        )
      )
    }
  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .renderBackend[Backend]
    .build

}
