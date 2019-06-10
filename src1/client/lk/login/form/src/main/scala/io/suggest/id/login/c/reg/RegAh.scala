package io.suggest.id.login.c.reg

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.common.empty.OptionUtil
import io.suggest.id.login.c.ILoginApi
import io.suggest.id.login.m.reg.step1.MReg1Captcha
import io.suggest.id.login.m.{EpwRegCaptchaSubmitResp, RegBackClick, RegNextClick}
import io.suggest.id.login.m.reg.{MRegS, MRegSteps}
import io.suggest.id.reg.MEpwRegCaptchaReq
import io.suggest.lk.m.CaptchaInit
import io.suggest.lk.m.captcha.MCaptchaS
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
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
                loginApi        : ILoginApi,
              )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопке "Далее".
    case m @ RegNextClick =>
      val v0 = value
      v0.step match {

        // Текущая страница - ввод реквизитов
        case MRegSteps.S0Creds =>
          // Проверить, всё ли правильно на странице ввода реквизитов
          if (v0.s0Creds.canSubmit) {
            var updF = MRegS.step.set( MRegSteps.S1Captcha )
            if (v0.s1Captcha.captcha.isEmpty)
              updF = updF andThen MRegS.s1Captcha
                .composeLens(MReg1Captcha.captcha)
                .set( Some(MCaptchaS.empty) )
            val v2 = updF(v0)
            // Надо проинициализировать форму капчи, если она ещё не готова:
            val fxOpt = OptionUtil.maybe( v2.s1Captcha.isCaptchaNeedsInit )( CaptchaInit.toEffectPure )
            ah.updatedMaybeEffect(v2, fxOpt)

          } else {
            LOG.warn( WarnMsgs.VALIDATION_FAILED, msg = (m, v0.s0Creds) )
            noChange
          }


        // Кнопка далее на странице капчи - надо запустить запрос капчи, наверное.
        case MRegSteps.S1Captcha =>
          if (v0.s1Captcha.submitReq.isReady) {
            // Капча уже принята.
            val v2 = MRegS.step
              .set( MRegSteps.S2SmsCode )(v0)
            updated(v2)

          } else if (v0.s1Captcha.canSubmit) {
            val captcha = v0.s1Captcha.captcha.get
            // Отработка состояния подформы капчи: отправить капчу-email-телефон на сервер.
            val timeStampMs = System.currentTimeMillis()
            val fx = Effect {
              val formData = MEpwRegCaptchaReq(
                email         = v0.s0Creds.email.value,
                phone         = v0.s0Creds.phone.value,
                captchaTyped  = captcha.typed.value,
                captchaSecret = captcha.contentReq.get.secret,
              )
              loginApi
                .epw2RegSubmit( formData )
                .transform { tryResp =>
                  Success( EpwRegCaptchaSubmitResp( timeStampMs, tryResp ) )
                }
            }

            val v2 = MRegS.s1Captcha
              .composeLens( MReg1Captcha.submitReq )
              .modify( _.pending(timeStampMs) )(v0)
            updated(v2, fx)

          } else {
            LOG.warn(WarnMsgs.VALIDATION_FAILED, msg = (m, v0.s1Captcha))
            noChange
          }


        // "Далее" нажато на странице ввода смс-кода
        case MRegSteps.S2SmsCode =>
          if (v0.s2SmsCode.submitReq.isReady) {
            // Уже проверенный смс-код.
            val v2 = MRegS.step
              .set( MRegSteps.S3CheckBoxes )(v0)
            updated(v2)

          } else if (v0.s2SmsCode.canSubmit) {
            val smsCode = v0.s2SmsCode.smsCode.get
            ???

          } else {
            LOG.warn( WarnMsgs.VALIDATION_FAILED, msg = (m, v0.s2SmsCode) )
            noChange
          }


        // Далее на последней странице галочек.
        case MRegSteps.S3CheckBoxes =>
          ???

      }

    // Реакция на клик по кнопке "назад" в форме регистрации.
    case m @ RegBackClick =>
      val v0 = value
      v0.step match {
        // Нельзя делать шаг назад с самого первого шага.
        case MRegSteps.S0Creds =>
          LOG.log(WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0.step))
          noChange
        case _ =>
          val step2 = MRegSteps.withValue( v0.step.value - 1 )
          val v2 = MRegS.step.set( step2 )( v0 )
          updated(v2)
      }


    // Результат запроса регистрации.
    case m: EpwRegCaptchaSubmitResp =>
      val v0 = value
      if (v0.s1Captcha.submitReq isPendingWithStartTime m.tstamp) {

        val v2 = m.resp.fold(
          // Серверу не удалось подтвердить соответствие капчи.
          // Необходимо подсветить поле красным, оставшись на текущей странице.
          {ex =>
            MRegS.s1Captcha.modify {
              MReg1Captcha.submitReq.modify( _.fail(ex) ) andThen
              MReg1Captcha.captcha
                .composeTraversal(Traversal.fromTraverse[Option, MCaptchaS])
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
            MRegS.step.set( MRegSteps.S2SmsCode )
          }
        )(v0)

        updated(v2)

      } else {
        LOG.warn( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }

  }

}
