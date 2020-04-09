package io.suggest.id.login.c.reg

import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.captcha.MCaptchaCheckReq
import io.suggest.common.empty.OptionUtil
import io.suggest.id.login.c.IIdentApi
import io.suggest.id.login.m.pwch.MPwNew
import io.suggest.id.login.m.reg.step0.MReg0Creds
import io.suggest.id.login.m.reg.step1.MReg1Captcha
import io.suggest.id.login.m.reg.step2.MReg2SmsCode
import io.suggest.id.login.m.reg.step4.MReg4SetPassword
import io.suggest.id.login.m.{RegBackClick, RegCaptchaSubmitResp, RegCredsSubmitResp, RegFinalSubmitResp, RegNextClick, RegSmsCheckResp}
import io.suggest.id.login.m.reg.{MRegS, MRegSteps}
import io.suggest.id.reg.{MCodeFormData, MCodeFormReq, MRegCreds0}
import io.suggest.lk.m.CaptchaInit
import io.suggest.lk.m.captcha.MCaptchaS
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.lk.m.sms.MSmsCodeS
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DoNothing
import monocle.Traversal
import scalaz.std.option._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 20:32
  * Description: Контроллер реги верхнего уровня.
  */
class RegAh[M](
                modelRW         : ModelRW[M, MRegS],
                pwNewRO         : ModelRO[MPwNew],
                loginApi        : IIdentApi,
              )
  extends ActionHandler( modelRW )
  with Log
{ ah =>


  /** Переход на страницу капчи.
    *
    * @param v0 Состояние модели.
    * @return ActionResult.
    */
  private def _toS1Captcha(v0: MRegS, updFs: List[MRegS => MRegS] = List.empty): ActionResult[M] = {
    var updFsAcc = MRegS.step.set( MRegSteps.S1Captcha ) :: updFs
    if (v0.s1Captcha.captcha.isEmpty)
      updFsAcc ::= MRegS.s1Captcha
        .composeLens(MReg1Captcha.captcha)
        .set( Some(MCaptchaS.empty) )
    val v2 = updFsAcc.reduce(_ andThen _)(v0)
    // Надо проинициализировать форму капчи, если она ещё не готова:
    val fxOpt = OptionUtil.maybe( v2.s1Captcha.isCaptchaNeedsInit )( CaptchaInit.toEffectPure )
    ah.updatedMaybeEffect(v2, fxOpt)
  }


  private def _toS2SmsCode(v0: MRegS): ActionResult[M] = {
    // Капча уже принята.
    val v2 = MRegS.step
      .set( MRegSteps.S2SmsCode )(v0)
    updated(v2)
  }


  private def _toS3CheckBoxes(v0: MRegS): ActionResult[M] = {
    val nextStep =
      if (v0.s3CheckBoxes.canSubmit) MRegSteps.S4SetPassword
      else MRegSteps.S3CheckBoxes
    val v2 = MRegS.step.set( nextStep )(v0)
    updated(v2)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопке "Далее".
    case m @ RegNextClick =>
      val v0 = value
      v0.step match {

        // Текущая страница - ввод реквизитов
        case MRegSteps.S0Creds =>
          // Проверить, всё ли правильно на странице ввода реквизитов
          if (v0.s0Creds.submitReq.isPending) {
            LOG.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.s0Creds.submitReq) )
            noChange

          } else if (v0.s2SmsCode.submitReq.isReady) {
            _toS3CheckBoxes(v0)

          } else if (v0.s1Captcha.submitReq.isReady) {
            _toS2SmsCode(v0)

          } else if (v0.s0Creds.submitReq.isReady) {
            // Реквест уже выполнен. Такое возможно, если юзер вернулся назад, и, не изменяя реквизиты, сразу вернулся вперёд.
            _toS1Captcha( v0 )

          } else if (v0.s0Creds.canSubmit) {
            // Отправить на сервер запрос за получением токена для данных реквизитов.
            val timeStampMs = System.currentTimeMillis()
            val fx = Effect {
              val reqBody = MRegCreds0(
                email = v0.s0Creds.email.value,
                phone = v0.s0Creds.phone.value,
              )
              loginApi
                .regStep0Submit( reqBody )
                .transform { tryResp =>
                  Success( RegCredsSubmitResp(timeStampMs, tryResp) )
                }
            }
            var updF =  MReg0Creds.submitReq
              .modify( _.pending(timeStampMs) )
            if (v0.s0Creds.pwRecoverMsg)
              updF = updF andThen MReg0Creds.pwRecoverMsg.set(false)

            val v2 = MRegS.s0Creds.modify(updF)( v0 )

            updated(v2, fx)

          } else {
            LOG.warn( ErrorMsgs.VALIDATION_FAILED, msg = (m, v0.s0Creds) )
            noChange
          }


        // Кнопка далее на странице капчи - надо запустить запрос капчи, наверное.
        case MRegSteps.S1Captcha =>
          if (v0.s2SmsCode.submitReq.isReady) {
            _toS3CheckBoxes(v0)

          } else if (v0.s1Captcha.submitReq.isReady) {
            _toS2SmsCode(v0)

          } else if (v0.s1Captcha.canSubmit) {
            val captcha = v0.s1Captcha.captcha.get
            // Отработка состояния подформы капчи: отправить капчу-email-телефон на сервер.
            val timeStampMs = System.currentTimeMillis()
            val fx = Effect {
              val formData = MCaptchaCheckReq(
                secret = captcha.contentReq.get.secret,
                typed  = captcha.typed.value,
              )
              loginApi
                .epw2RegSubmit( formData )
                .transform { tryResp =>
                  Success( RegCaptchaSubmitResp( timeStampMs, tryResp ) )
                }
            }

            val v2 = MRegS.s1Captcha
              .composeLens( MReg1Captcha.submitReq )
              .modify( _.pending(timeStampMs) )(v0)
            updated(v2, fx)

          } else {
            LOG.warn(ErrorMsgs.VALIDATION_FAILED, msg = (m, v0.s1Captcha))
            noChange
          }


        // "Далее" нажато на странице ввода смс-кода
        case MRegSteps.S2SmsCode =>
          if (v0.s2SmsCode.submitReq.isReady) {
            // TODO Если юзер уже существует на сервере, то переходить сразу на шаг выставления пароля, даже если галочки не выставлены.
            _toS3CheckBoxes(v0)

          } else if (v0.s2SmsCode.canSubmit) {
            val tstampMs = System.currentTimeMillis()
            val fx = Effect {
              val data = MCodeFormReq(
                token    = v0.s1Captcha.submitReq.get.token,
                formData = MCodeFormData(
                  code = Some( v0.s2SmsCode.smsCode.get.typed.value ),
                ),
              )
              loginApi
                .smsCodeCheck(data)
                .transform { tryResp =>
                  Success( RegSmsCheckResp(tstampMs, tryResp) )
                }
            }

            val v2 = MRegS.s2SmsCode
              .composeLens( MReg2SmsCode.submitReq )
              .modify( _.pending(tstampMs) )(v0)
            updated(v2, fx)

          } else {
            LOG.warn( ErrorMsgs.VALIDATION_FAILED, msg = (m, v0.s2SmsCode) )
            noChange
          }


        // Далее - страница галочек.
        case MRegSteps.S3CheckBoxes =>
          if (!v0.s3CheckBoxes.canSubmit) {
            // Реквест уже запущен. Вероятно, повторный сигнал сабмита по ошибке пришёл.
            LOG.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (v0.step, v0.s3CheckBoxes) )
            noChange
          } else {
            // Просто перейти на следующий шаг.
            val v2 = MRegS.step.set( MRegSteps.S4SetPassword )(v0)
            updated(v2)
          }


        // Выставление пароля.
        case MRegSteps.S4SetPassword =>
          // Надо отправить на сервер подтверждение выбранных галочек.
          val pwNew = pwNewRO.value
          if (!pwNew.canSubmit || !v0.s4SetPassword.canSubmit) {
            LOG.log( ErrorMsgs.VALIDATION_FAILED, msg = (v0.step, v0.s4SetPassword) )
            noChange
          } else {
            val tstampMs = System.currentTimeMillis()
            val fx = Effect {
              val data = MCodeFormReq(
                token    = v0.s2SmsCode.submitReq.get.token,
                formData = MCodeFormData(
                  code = Some( pwNew.passwordValue ),
                ),
              )
              loginApi
                .regFinalSubmit( data )
                .transform { tryResp =>
                  Success( RegFinalSubmitResp(tstampMs, tryResp) )
                }
            }

            val v2 = MRegS.s4SetPassword
              .composeLens( MReg4SetPassword.submitReq )
              .modify( _.pending(tstampMs) )(v0)
            updated(v2, fx)
          }

      }


    // Результат запроса на сервер с реквизитами регистрации.
    case m: RegCredsSubmitResp =>
      val v0 = value
      if (!(v0.s0Creds.submitReq isPendingWithStartTime m.tstamp)) {
        LOG.log( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange

      } else {
        // Пора залить результат в состоянии.
        var updF = MRegS.s0Creds
          .composeLens( MReg0Creds.submitReq )
          .modify( _.withTry(m.resp) )

        if (m.resp.isSuccess) {
          _toS1Captcha( v0, updF :: Nil )
        } else {
          val v2 = updF(v0)
          updated(v2)
        }
      }


    // Реакция на клик по кнопке "назад" в форме регистрации.
    case m @ RegBackClick =>
      val v0 = value
      v0.step match {
        // Нельзя делать шаг назад с самого первого шага.
        case MRegSteps.S0Creds =>
          LOG.log(ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.step))
          noChange
        // Переход сразу на первый шаг, где вводятся реквизиты, т.к. возвращаться на выверенную капчу
        // или выверенный смс-код смысла нет, равно как и на форму уже принятых галочек (?).
        case _ =>
          val step2 = MRegSteps.S0Creds
          val v2 = MRegS.step.set( step2 )( v0 )
          updated(v2)
      }


    // Результат запроса регистрации.
    case m: RegCaptchaSubmitResp =>
      val v0 = value
      if (v0.s1Captcha.submitReq isPendingWithStartTime m.tstamp) {

        val v2 = m.resp.fold(
          // Серверу не удалось подтвердить соответствие капчи.
          // Необходимо подсветить поле красным, оставшись на текущей странице.
          {ex =>
            MRegS.s1Captcha.modify {
              MReg1Captcha.submitReq.modify( _.fail(ex) ) andThen
              MReg1Captcha.captcha
                .composeTraversal( Traversal.fromTraverse[Option, MCaptchaS] )
                .composeLens( MCaptchaS.typed )
                .composeLens( MTextFieldS.isValid )
                .set(false)
            }
          },
          {okResp =>
            // Сервер подтвердил капчу.
            MRegS.s1Captcha
              .composeLens( MReg1Captcha.submitReq )
              .modify( _.ready(okResp) ) andThen
            MRegS.step.set( MRegSteps.S2SmsCode ) andThen
            MRegS.s2SmsCode
              .composeLens(MReg2SmsCode.smsCode)
              .set( Some(MSmsCodeS.empty) )
          }
        )(v0)

        updated(v2)

      } else {
        LOG.warn( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }


    // Результат запроса проверки смс-кода.
    case m: RegSmsCheckResp =>
      val v0 = value

      if (!(v0.s2SmsCode.submitReq isPendingWithStartTime m.tstamp)) {
        LOG.warn( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = (m, v0.s2SmsCode.submitReq) )
        noChange

      } else {
        var updAccF = MRegS.s2SmsCode
          .composeLens( MReg2SmsCode.submitReq )
          .modify( _.withTry(m.tryResp) )

        for (_ <- m.tryResp) {
          updAccF = updAccF andThen
            MRegS.step.set( MRegSteps.S3CheckBoxes )
        }

        val v2 = updAccF( v0 )
        updated(v2)
      }


    // Результат запроса окончания регистрации.
    case m: RegFinalSubmitResp =>
      val v0 = value

      if (!(v0.s4SetPassword.submitReq isPendingWithStartTime m.tstamp)) {
        LOG.warn( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = (m, v0.s4SetPassword.submitReq) )
        noChange

      } else {
        // Обработать ответ сервера.
        val v2 = MRegS.s4SetPassword
          .composeLens( MReg4SetPassword.submitReq )
          .modify( _.withTry(m.tryResp) )(v0)

        // Запустить эффект редиректа по принятой ссылке при положительном ответе сервера:
        val fxOpt = for (okResp <- m.tryResp.toOption) yield {
          Effect.action {
            DomQuick.goToLocation( okResp.token )
            DoNothing
          }
        }

        ah.updatedMaybeEffect(v2, fxOpt)
      }

  }

}
