package io.suggest.lk.c

import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.lk.m._
import io.suggest.lk.m.captcha.MCaptchaS
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits._
import org.scalajs.dom.raw.URL
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.03.19 20:43
  * Description: Контроллер состояния капчи.
  */

class CaptchaAh[M](
                    api           : ICaptchaApi,
                    idTokenRO     : ModelRO[String],
                    modelRW       : ModelRW[M, Option[MCaptchaS]],
                  )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Капчу проинициализировать/переинициализировать:
    case CaptchaInit =>
      val v0 = value

      val tstampMs = System.currentTimeMillis()
      val fx = Effect {
        api
          .getCaptcha( idTokenRO.value )
          .transform { tryResp =>
            Success( CaptchaInitRes( tryResp, tstampMs ) )
          }
      }

      val v1 = v0.getOrElse( MCaptchaS.empty )
      val v2 = MCaptchaS.req
        .modify( _.pending( tstampMs ) )(v1)
      updated(Some(v2), fx)


    // Пришёл результат запроса капчи с сервера.
    case m: CaptchaInitRes =>
      value.fold {
        LOG.warn( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange

      } { v0 =>
        if (!(v0.contentReq isPendingWithStartTime m.timeStampMs)) {
          // Ответ пришёл, но не тот, который был запущен:
          LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
          noChange

        } else {
          var changesAccF = MCaptchaS.req.modify( _.withTry(m.tryResp) )

          // Если успешный ответ, то надо заменить blob-ссылку на картинку.
          for (res <- m.tryResp) {
            // Есть новая картинка капчи - пора очистить старую ссылку:
            for (oldUrl <- v0.captchaImgUrlOpt)
              URL.revokeObjectURL( oldUrl )
            // Залить новую ссылку в состояние
            val blobUrl2 = URL.createObjectURL( res.imgData )
            changesAccF = changesAccF andThen MCaptchaS.captchaImgUrlOpt.set( Some(blobUrl2) )
          }

          // Обновить состояние:
          val v2 = changesAccF(v0)
          updated( Some(v2) )
        }
      }


    // Ввод значения капчи.
    case m: CaptchaTyped =>
      value.fold {
        LOG.warn( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange
      } { v0 =>
        if (v0.typed.value ==* m.typed) {
          // Текст капчи не изменился.
          noChange
        } else {
          var updAccF = MTextFieldS.value.set( m.typed )
          if (!v0.typed.isValid && MCaptchaS.isTypedCapchaValid(v0.typed.value))
            updAccF = updAccF andThen MTextFieldS.isValid.set(true)

          val v2 = MCaptchaS.typed
            .modify( updAccF )(v0)

          updated( Some(v2) )
        }
      }


    // Сигнал разфокусировки инпута капчи.
    case m @ CaptchaInputBlur =>
      // Проверить, есть ли валидный текст на капче.
      value.fold {
        LOG.warn( WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = m)
        noChange
      } { v0 =>
        var inputChangesAccF = List.empty[MTextFieldS => MTextFieldS]

        // Поверхностная проверка введённого текста капчи.
        if (
          v0.typed.isValid &&
          !MCaptchaS.isTypedCapchaValid(v0.typed.value)
        ) {
          // Капча стала невалидной.
          inputChangesAccF ::= MTextFieldS.isValid.set( false )
        }

        // Тримминг начала и хвоста капчи.
        if (v0.typed.value.nonEmpty) {
          val trimmedTyped = v0.typed.value.trim
          if ( trimmedTyped !=* v0.typed.value )
            inputChangesAccF ::= MTextFieldS.value.set( trimmedTyped )
        }

        inputChangesAccF
          .reduceOption(_ andThen _)
          .fold(noChange) { updateF =>
            val v2 = MCaptchaS.typed.modify( updateF )(v0)
            updated( Some(v2) )
          }
      }

  }

}
