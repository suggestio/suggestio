package io.suggest.id.login.v.reg

import chandu0101.scalajs.react.components.materialui.{Mui, MuiButton, MuiButtonProps, MuiButtonSizes, MuiFormControl, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps, MuiMobileStepper, MuiMobileStepperClasses, MuiMobileStepperProps, MuiMobileStepperVariants}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.reg.step0.MReg0Creds
import io.suggest.id.login.m.reg.step1.MReg1Captcha
import io.suggest.id.login.m.reg.step2.MReg2SmsCode
import io.suggest.id.login.m.{RegBackClick, RegNextClick}
import io.suggest.id.login.m.reg.{MRegS, MRegStep, MRegSteps}
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.v.stuff.{ButtonR, LoginProgressR}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ScalaComponent, raw}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

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
            loginFormCssCtxP   : React.Context[LoginFormCss],
          ) {

  type Props_t = MRegS
  type Props = ModelProxy[Props_t]


  case class State(
                    isSubmitPendingSomeC    : ReactConnectProxy[Some[Boolean]],
                    activeStepC             : ReactConnectProxy[MRegStep],
                    backBtnDisabledC        : ReactConnectProxy[Some[Boolean]],
                    nextBtnDisabledC        : ReactConnectProxy[Some[Boolean]],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private val _onNextClick: Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, RegNextClick )
    private val _onNextClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent => _onNextClick }

    private val _onBackClick: Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, RegBackClick )
    private val _onBackClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent => _onBackClick }


    def render(p: Props, s: State): VdomElement = {

      // Шаг ввода начальных реквизитов:
      lazy val step0Creds  = p.wrap(_.s0Creds  )(reg0CredsR.component.apply  )(implicitly, MReg0Creds.MReg0CredsFastEq)

      // Шаг ввода капчи:
      lazy val stepCaptcha = p.wrap(_.s1Captcha)(reg1CaptchaR.component.apply)(implicitly, MReg1Captcha.MReg1CaptchaFastEq)

      // Шаг ввода смс-кода.
      lazy val stepSmsCode = p.wrap(_.s2SmsCode)(reg2SmsCodeR.component.apply)(implicitly, MReg2SmsCode.MReg2SmsCodeFastEq)

      // Шаг галочек соглашений и окончания регистрации
      //val stepCheckBoxes = ???

      // Сообщения текстовые зависят от common-контекста.
      val backMsg = commonReactCtxProv.consume { commonReactCtx =>
        commonReactCtx.messages( MsgCodes.`Back` )
      }
      val nextMsg = commonReactCtxProv.consume { commonReactCtx =>
        commonReactCtx.messages( MsgCodes.`Next` )
      }

      val backBtn = s.backBtnDisabledC { disabledSomeProxy =>
        MuiButton(
          new MuiButtonProps {
            override val size     = MuiButtonSizes.small
            override val disabled = disabledSomeProxy.value.value
            override val onClick  = _onBackClickCbF
          }
        )(
          Mui.SvgIcons.KeyboardArrowLeft()(),
          backMsg,
        )
      }

      val nextBtn = s.nextBtnDisabledC { disabledSomeProxy =>
        MuiButton(
          new MuiButtonProps {
            override val size     = MuiButtonSizes.small
            override val disabled = disabledSomeProxy.value.value
            override val onClick  = _onNextClickCbF
          }
        )(
          nextMsg,
          Mui.SvgIcons.KeyboardArrowRight()(),
        )
      }

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

          // Прогресс-бар ожидания...
          s.isSubmitPendingSomeC { loginProgressR.component.apply },

          // Вся пошаговая рега здесь
          loginFormCssCtxP.consume { loginFormCssCtx =>
            // Стили для переключателя шагов
            val stepperCss = new MuiMobileStepperClasses {
              override val root = loginFormCssCtx.regStepper.htmlClass
            }

            s.activeStepC { activeStepProxy =>
              val _activeStep = activeStepProxy.value

              <.div(
                _activeStep match {
                  case MRegSteps.S0Creds      => step0Creds
                  case MRegSteps.S1Captcha    => stepCaptcha
                  case MRegSteps.S2SmsCode    => stepSmsCode
                  case MRegSteps.S3CheckBoxes => ???
                },

                MuiMobileStepper.component(
                  new MuiMobileStepperProps {
                    override val steps      = MRegSteps.values.length
                    override val activeStep = _activeStep.value
                    override val variant    = MuiMobileStepperVariants.dots
                    override val backButton = backBtn.rawNode
                    override val nextButton = nextBtn.rawNode
                    override val classes    = stepperCss
                  }
                ),
              )

            }
          },

        )
      )

      //MuiPaper()(
        s.isSubmitPendingSomeC { isFormDisabledSomeProxy =>
          <.form(
            ReactCommonUtil.maybe( isFormDisabledSomeProxy.value.value ) {
              TagMod(
                ^.onSubmit --> _onNextClick,
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
        isSubmitPendingSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.hasSubmitReqPending )
        }( FastEq.AnyRefEq ),
        activeStepC = propsProxy.connect(_.step)( FastEq.AnyRefEq ),
        backBtnDisabledC = propsProxy.connect { props =>
          // Кнопка "Назад" выключена на нулевом шаге.
          OptionUtil.SomeBool( props.step.value <= 0 )
        }( FastEq.AnyRefEq ),
        nextBtnDisabledC = propsProxy.connect { props =>
          // Кнопка "Далее" выключена, если на текущем шаге нет готовности данных.
          OptionUtil.SomeBool( !props.stepState.canSubmit )
        }
      )
    }
    .renderBackend[Backend]
    .build


  def apply( propsProxy: Props ) = component( propsProxy )

}
