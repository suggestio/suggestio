package io.suggest.id.login.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.id.login.m.{EpwRegSubmit, EpwRegSubmitResp, RegEmailBlur, RegEmailEdit}
import io.suggest.id.login.m.reg.MEpwRegS
import io.suggest.id.reg.MEpwRegReq
import io.suggest.lk.m.MTextFieldS
import io.suggest.msg.WarnMsgs
import io.suggest.text.Validators
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.19 12:15
  * Description: Контроллер регистрации по имени-паролю.
  */
class EpwRegAh[M](
                   modelRW        : ModelRW[M, MEpwRegS],
                   loginApi       : ILoginApi,
                 )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Редактирования поля email.
    case m: RegEmailEdit =>
      val v0 = value
      if (v0.email.value ==* m.email) {
        noChange

      } else {
        var fieldUpdatesAccF = MTextFieldS.value.set(m.email)

        // Если isValid уже false, надо повторить валидацию.
        if (!v0.email.isValid && Validators.isEmailValid( m.email ))
          fieldUpdatesAccF = fieldUpdatesAccF andThen MTextFieldS.isValid.set( true )

        val v2 = MEpwRegS.email
          .modify( fieldUpdatesAccF )(v0)

        updated(v2)
      }


    // Расфокусировка поля email. Надо проверить валидность поля.
    case RegEmailBlur =>
      val v0 = value
      if (v0.email.isValid && !Validators.isEmailValid( v0.email.value )) {
        val v2 = MEpwRegS.email
          .composeLens( MTextFieldS.isValid )
          .set( false )(v0)
        updated( v2 )

      } else {
        noChange
      }


    // Клик по кнопке регистрации.
    case m @ EpwRegSubmit =>
      val v0 = value

      if (v0.submitReq.isPending) {
        LOG.log( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = m )
        noChange

      } else {
        val timeStampMs = System.currentTimeMillis()
        val fx = Effect {
          val formData = MEpwRegReq(
            email         = v0.email.value,
            captchaTyped  = v0.captcha.typed.value,
            captchaSecret = v0.captcha.req.get.secret,
          )
          loginApi
            .epw2RegSubmit( formData )
            .transform { tryResp =>
              Success( EpwRegSubmitResp( timeStampMs, tryResp ) )
            }
        }
        val v2 = MEpwRegS.submitReq
          .modify( _.pending(timeStampMs) )(v0)
        updated(v2, fx)
      }


    // Результат запроса регистрации.
    case m: EpwRegSubmitResp =>
      val v0 = value
      if (v0.submitReq isPendingWithStartTime m.tstamp) {
        val v2 = MEpwRegS.submitReq
          .modify( _.ready("") )(v0)
        updated(v2)

      } else {
        LOG.warn( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }

  }

}
