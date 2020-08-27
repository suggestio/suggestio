package io.suggest.id.login.c.pwch

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.err.MCheckException
import io.suggest.id.login.c.IIdentApi
import io.suggest.id.login.m.{PwChangeSubmitRes, PwVisibilityChange, RegNextClick}
import io.suggest.id.login.m.pwch.{MPwChangeS, MPwNew}
import io.suggest.id.pwch.MPwChangeForm
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.spa.DiodeUtil.Implicits._
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.19 16:25
  * Description: Общий контроллер формы смены пароля.
  */
class PwChangeAh[M](
                     identApi   : IIdentApi,
                     modelRW    : ModelRW[M, MPwChangeS],
                   )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Нажата кнопка сохранения.
    case m @ RegNextClick =>
      val v0 = value

      if (!v0.canSubmit) {
        logger.warn( ErrorMsgs.VALIDATION_FAILED, msg = (m, v0) )
        noChange

      } else if (v0.submitReq.isPending) {
        logger.warn( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.submitReq) )
        noChange

      } else {
        val tstamp = System.currentTimeMillis()
        val fx = Effect {
          val data = MPwChangeForm(
            pwOld = v0.pwOld.value,
            pwNew = v0.pwNew.passwordValue,
          )
          identApi
            .pwChangeSubmit( data )
            .transform { tryRes =>
              Success( PwChangeSubmitRes(tstamp, tryRes) )
            }
        }

        val v2 = MPwChangeS.submitReq
          .modify( _.pending(tstamp) )(v0)

        updated(v2, fx)
      }


    // Поступил результат запроса изменения пароля.
    case m: PwChangeSubmitRes =>
      val v0 = value
      if (!(v0.submitReq isPendingWithStartTime m.timestampMs)) {
        logger.warn( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = (m, v0.submitReq) )
        noChange

      } else {
        val v2 = m.tryRes.fold(
          {ex =>
            var updAccF = MPwChangeS.submitReq.modify( _.withTry(m.tryRes) )
            ex match {
              case mce: MCheckException =>
                // Отрендерить экзепшен
                if (mce.fields contains[String] MPwChangeForm.Fields.PW_OLD_FN)
                  updAccF = updAccF andThen (MPwChangeS.pwOld composeLens MTextFieldS.isValid set false)
                if (mce.fields contains[String] MPwChangeForm.Fields.PW_NEW_FN) {
                  val isValidSetF = MTextFieldS.isValid set false
                  updAccF = updAccF andThen
                    (MPwChangeS.pwNew modify (MPwNew.password1.modify(isValidSetF) andThen MPwNew.password2.modify(isValidSetF)))
                }
              case _ =>
            }
            updAccF( v0 )
          },
          {_ =>
            v0.copy(
              pwOld     = MTextFieldS.empty,
              pwNew     = MPwNew.empty,
              submitReq = v0.submitReq.withTry( m.tryRes ),
            )
          }
        )

        // Отредиректить юзера куда-нибудь? Или просто сделать "пароль изменён".
        updated( v2 )
      }


    case m: PwVisibilityChange =>
      val v0 = value

      val lens = if (m.isPwNew) {
        MPwChangeS.pwNew
          .composeLens( MPwNew.pwVisible )
      } else {
        MPwChangeS.pwOldVisible
      }

      if (lens.get(v0) ==* m.visible) {
        noChange
      } else {
        val v2 = lens.set( m.visible )(v0)
        updated(v2)
      }

  }

}
