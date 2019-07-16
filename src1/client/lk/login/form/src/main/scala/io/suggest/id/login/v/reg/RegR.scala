package io.suggest.id.login.v.reg

import com.materialui.{Mui, MuiButton, MuiButtonProps, MuiButtonSizes, MuiFormControl, MuiFormControlProps, MuiFormGroup, MuiFormGroupProps, MuiMobileStepper, MuiMobileStepperClasses, MuiMobileStepperProps, MuiMobileStepperVariants}
import diode.{FastEq, UseValueEq}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.reg.step0.MReg0Creds
import io.suggest.id.login.m.reg.step1.MReg1Captcha
import io.suggest.id.login.m.reg.step3.MReg3CheckBoxes
import io.suggest.id.login.m.{MLoginRootS, RegBackClick, RegNextClick}
import io.suggest.id.login.m.reg.{MRegStep, MRegSteps}
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.v.stuff.LoginProgressR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sjs.common.empty.JsOptionUtil
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ScalaComponent}
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
            reg3CheckBoxesR    : Reg3CheckBoxesR,
            reg4SetPasswordR   : Reg4SetPasswordR,
            loginProgressR     : LoginProgressR,
            commonReactCtxProv : React.Context[MCommonReactCtx],
            loginFormCssCtxP   : React.Context[LoginFormCss],
          ) {

  type Props_t = MLoginRootS
  type Props = ModelProxy[Props_t]


  /** Пропертисы для коннекшена next-кнопки.
    *
    * @param disabled выключена?
    * @param isFinal последний шаг?
    */
  case class MNextBtnProps(
                            disabled   : Boolean,
                            isFinal    : Boolean,
                          )
    extends UseValueEq


  case class State(
                    isSubmitPendingSomeC    : ReactConnectProxy[Some[Boolean]],
                    activeStepC             : ReactConnectProxy[MRegStep],
                    backBtnDisabledC        : ReactConnectProxy[Some[Boolean]],
                    nextBtnPropsC           : ReactConnectProxy[MNextBtnProps],
                    isLastStepSomeC         : ReactConnectProxy[Some[Boolean]],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private val _onNextClick: Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, RegNextClick )
    private val _onNextClickCbF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent => _onNextClick }

    private def _onFormSubmit(e: ReactEvent): Callback =
      ReactCommonUtil.preventDefaultCB(e) >> _onNextClick

    private def _onBackClick(e: ReactEvent): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, RegBackClick )
    private val _onBackClickCbF = ReactCommonUtil.cbFun1ToJsCb( _onBackClick )


    def render(pRoot: Props, s: State): VdomElement = {

      val p = pRoot.zoom(_.reg)

      // Шаг ввода начальных реквизитов:
      lazy val step0Creds  = p.wrap(_.s0Creds  )(reg0CredsR.component.apply  )(implicitly, MReg0Creds.MReg0CredsFastEq)

      // Шаг ввода капчи:
      lazy val stepCaptcha = p.wrap(_.s1Captcha)(reg1CaptchaR.component.apply)(implicitly, MReg1Captcha.MReg1CaptchaFastEq)

      // Шаг ввода смс-кода.
      lazy val stepSmsCode = reg2SmsCodeR.component( p )

      // Шаг галочек соглашений и окончания регистрации
      lazy val stepCheckBoxes = p.wrap(_.s3CheckBoxes)(reg3CheckBoxesR.component.apply)(implicitly, MReg3CheckBoxes.MReg3CheckBoxesFastEq)

      // финальный шаг выставления пароля.
      lazy val stepSetPassword = reg4SetPasswordR.component( pRoot )

      // Сообщение кнопки "Назад".
      val backMsg = commonReactCtxProv.consume { commonReactCtx =>
        commonReactCtx.messages( MsgCodes.`Back` )
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

      // Сообщение кнопки "Вперёд". На последнем шаге надо "Завершить".
      val nextMsg = commonReactCtxProv.consume { commonReactCtx =>
        s.isLastStepSomeC { isLastStepSomeProxy =>
          val msgCode =
            if (isLastStepSomeProxy.value.value) MsgCodes.`_to.Finish`
            else MsgCodes.`Next`
          <.span(
            commonReactCtx.messages( msgCode )
          )
        }
      }
      val nextBtn = s.nextBtnPropsC { nextBtnPropsProxy =>
        val nextBtnProps = nextBtnPropsProxy.value
        // Здесь кнопка имеет двойственность: "Далее" либо "Завершить". Вторая делает сабмит формы, который перехватывается.
        // Это нужно, чтобы подсказать браузеру состояние сабмита пароля для запоминания пароля в менеджере паролей.
        val isNotFinal = !nextBtnProps.isFinal
        val _onClickUnd = JsOptionUtil.maybeDefined(isNotFinal)(_onNextClickCbF)
        val _type =
          if (nextBtnProps.isFinal) HtmlConstants.Input.submit
          else HtmlConstants.Input.button

        MuiButton {
          new MuiButtonProps {
            override val size     = MuiButtonSizes.small
            override val disabled = nextBtnProps.disabled
            override val onClick  = _onClickUnd
            override val `type`   = _type
          }
        } (
          nextMsg,
          ReactCommonUtil.maybeNode( isNotFinal )(
            Mui.SvgIcons.KeyboardArrowRight()()
          )
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
                  case MRegSteps.S0Creds        => step0Creds
                  case MRegSteps.S1Captcha      => stepCaptcha
                  case MRegSteps.S2SmsCode      => stepSmsCode
                  case MRegSteps.S3CheckBoxes   => stepCheckBoxes
                  case MRegSteps.S4SetPassword  => stepSetPassword
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

      s.isSubmitPendingSomeC { isSubmitPendingSomeProxy =>
        val isBusy = isSubmitPendingSomeProxy.value.value
        <.form(
          // Сабмит формы идентичен NextClick, возможен на любом шаге.
          if (isBusy)
            ^.disabled  := true
          else
            ^.onSubmit ==> _onFormSubmit,

          formContent,
        )
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mrootProxy =>
      val regProxy = mrootProxy.zoom(_.reg)

      val lastStep = MRegSteps.values.last

      State(

        isSubmitPendingSomeC = regProxy.connect { props =>
          OptionUtil.SomeBool( props.hasSubmitReqPending )
        }( FastEq.AnyRefEq ),

        activeStepC = regProxy.connect(_.step)( FastEq.AnyRefEq ),

        backBtnDisabledC = regProxy.connect { props =>
          // Кнопка "Назад" выключена на нулевом шаге.
          OptionUtil.SomeBool( props.step.value <= 0 )
        }( FastEq.AnyRefEq ),

        nextBtnPropsC = {
          mrootProxy.connect { mroot =>
            val r = mroot.reg
            MNextBtnProps(
              // Кнопка "Далее" выключена, если на текущем шаге нет готовности данных.
              disabled = {
                val canSubmit = r.step match {
                  case MRegSteps.S0Creds       => r.s0Creds.canSubmit
                  case MRegSteps.S1Captcha     => r.s1Captcha.canSubmit
                  case MRegSteps.S2SmsCode     => r.s2SmsCode.canSubmit
                  case MRegSteps.S3CheckBoxes  => r.s3CheckBoxes.canSubmit
                  case MRegSteps.S4SetPassword =>
                    mroot.overall.pwNew.canSubmit && r.s4SetPassword.canSubmit
                }
                !canSubmit
              },
              // Кнопка "Завершить" на последнем шаге.
              isFinal  = r.step ==* lastStep,
            )
          }( FastEq.ValueEq )
        },

        // Это последний шаг?
        isLastStepSomeC = {
          val lastStep = MRegSteps.values.last
          regProxy.connect { props =>
            OptionUtil.SomeBool( props.step ==* lastStep )
          }( FastEq.AnyRefEq )
        }
      )
    }
    .renderBackend[Backend]
    .build


  def apply( propsProxy: Props ) = component( propsProxy )

}
