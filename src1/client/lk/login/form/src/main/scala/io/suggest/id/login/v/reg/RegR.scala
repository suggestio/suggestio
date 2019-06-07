package io.suggest.id.login.v.reg

import chandu0101.scalajs.react.components.materialui.{MuiFormControl, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps, MuiPaper, MuiStep, MuiStepLabel, MuiStepProps, MuiStepper, MuiStepperOrientations, MuiStepperProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.reg.step0.MReg0Creds
import io.suggest.id.login.m.reg.step1.MReg1Captcha
import io.suggest.id.login.m.reg.step2.MReg2SmsCode
import io.suggest.id.login.m.RegNextClick
import io.suggest.id.login.m.reg.{MRegS, MRegStep, MRegSteps}
import io.suggest.id.login.v.stuff.{ButtonR, LoginProgressR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.{BackendScope, Callback, React, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.19 16:22
  * Description: Форма начала регистрации по имени-паролю.
  */
class RegR(
            reg0CredsR         : Reg0CredsR,
            reg1CaptchaR       : Reg1CaptchaR,
            reg2SmsCodeR       : Reg2SmsCodeR,
            buttonR            : ButtonR,
            loginProgressR     : LoginProgressR,
            commonReactCtxProv : React.Context[MCommonReactCtx],
          ) {

  type Props_t = MRegS
  type Props = ModelProxy[Props_t]


  case class State(
                    isSubmitPendingSomeC    : ReactConnectProxy[Some[Boolean]],
                    activeStepC             : ReactConnectProxy[MRegStep],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private val _onFormSubmit: Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, RegNextClick )

    def render(p: Props, s: State): VdomElement = {

      val steps = List(
        MRegSteps.S0Creds     -> p.wrap(_.s0Creds  )(reg0CredsR.component.apply  )(implicitly, MReg0Creds.MReg0CredsFastEq),
        MRegSteps.S1Captcha   -> p.wrap(_.s1Captcha)(reg1CaptchaR.component.apply)(implicitly, MReg1Captcha.MReg1CaptchaFastEq),
        MRegSteps.S2SmsCode   -> p.wrap(_.s2SmsCode)(reg2SmsCodeR.component.apply)(implicitly, MReg2SmsCode.MReg2SmsCodeFastEq),
      )


      // Шаг ввода начальных реквизитов:
      //val step0 = p.wrap(_.s0Creds)(reg0CredsR.component.apply)(implicitly, MReg0Creds.MReg0CredsFastEq)

      // Шаг ввода капчи:
      //val stepCaptcha = p.wrap(_.s1Captcha)(reg1CaptchaR.component.apply)(implicitly, MReg1Captcha.MReg1CaptchaFastEq)

      // Шаг ввода смс-кода.
      //val stepSmsCode = p.wrap(_.s2SmsCode)(reg2SmsCodeR.component.apply)(implicitly, MReg2SmsCode.MReg2SmsCodeFastEq)

      // Шаг галочек соглашений и окончания регистрации
      //val stepCheckBoxes = ???


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

          // Вся пошаговая рега здесь
          s.activeStepC { activeStepProxy =>
            val _activeStep = activeStepProxy.value
            MuiStepper(
              new MuiStepperProps {
                override val orientation = MuiStepperOrientations.vertical
                override val activeStep  = _activeStep.value
              }
            )(
              steps.toVdomArray { case (regStep, stepContent) =>
                MuiStep.component.withKey(regStep.value)(
                  new MuiStepProps {
                    override val active = (regStep ===* _activeStep)
                  }
                )(
                  MuiStepLabel()(
                    commonReactCtxProv.consume { ctx =>
                      ctx.messages( regStep.stepLabelCode )
                    }
                  ),
                  stepContent,
                )
              }
            )
          },

          // Прогресс-бар ожидания...
          s.isSubmitPendingSomeC { loginProgressR.component.apply },

          // Кнопка "Далее".
          p.wrap { props =>
            buttonR.PropsVal(
              disabled = false, // props.disableSubmit,
              onClick  = RegNextClick,
              msgCode  = MsgCodes.`Next`,
            )
          }(buttonR.apply)(implicitly, buttonR.ButtonRPropsValFastEq)
        )
      )

      //MuiPaper()(
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
      //)

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isSubmitPendingSomeC = propsProxy.connect(_.hasSubmitReqPendingSome)( FastEqUtil.RefValFastEq ),
        activeStepC = propsProxy.connect(_.step)( FastEq.AnyRefEq ),
      )
    }
    .renderBackend[Backend]
    .build


  def apply( propsProxy: Props ) = component( propsProxy )

}
