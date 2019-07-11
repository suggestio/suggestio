package io.suggest.id.login.c.pwch

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.id.login.c.IIdentApi
import io.suggest.id.login.m.{PwChangeSubmitRes, RegNextClick}
import io.suggest.id.login.m.pwch.{MPwChangeS, MPwNew}
import io.suggest.id.pwch.MPwChangeForm
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.19 16:25
  * Description: Общий контроллер формы смены пароля.
  */
class PwChangeAh[M](
                     api        : IIdentApi,
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
        LOG.warn( WarnMsgs.VALIDATION_FAILED, msg = (m, v0) )
        noChange

      } else if (v0.submitReq.isPending) {
        LOG.warn( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, v0.submitReq) )
        noChange

      } else {
        val tstamp = System.currentTimeMillis()
        val fx = Effect {
          val data = MPwChangeForm(
            pwOld = v0.pwOld.value,
            pwNew = v0.pwNew.passwordValue,
          )
          api
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
        LOG.warn( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = (m, v0.submitReq) )
        noChange

      } else {
        val submitReq2 = v0.submitReq.withTry( m.tryRes )

        val v2 = if (submitReq2.isFailed) {
          // Просто отрендерить ошибку
          MPwChangeS.submitReq
            .modify( _.withTry(m.tryRes) )(v0)
        } else {
          v0.copy(
            pwOld     = MTextFieldS.empty,
            pwNew     = MPwNew.empty,
            submitReq = submitReq2,
          )
        }

        // Отредиректить юзера куда-нибудь? Или просто сделать "пароль изменён".
        updated( v2 )
      }

  }

}
