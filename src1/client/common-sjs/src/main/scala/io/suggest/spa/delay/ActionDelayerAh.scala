package io.suggest.spa.delay

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 17:26
  * Description: Контроллер управления отложенными экшенами.
  */
class ActionDelayerAh[M](stateRW: ModelRW[M, MDelayerS])
  extends ActionHandler(stateRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Команда к откладывания экшена для повторного исполнения через какое-то время.
    case m: DelayAction =>
      // Организуем откладывание экшена на потом:
      val v0 = value
      val actionId = v0.counter
      val counter1 = v0.counter + 1
      val fx = Effect {
        DomQuick
          .timeoutPromiseT(m.delayMs)( FireDelayedAction(actionId) )
          .fut
      }

      // Сохраняем итоги деятельности в состояние.
      val a2 = MDelayedAction( m )
      val v2 = v0.copy(
        counter = counter1,
        delayed = v0.delayed.updated(actionId, a2)
      )
      updated(v2, fx)


    // Настала пора исполнить отложенный на потом экшен:
    case m: FireDelayedAction =>
      val v0 = value
      v0.delayed.get(m.actionId).fold {
        LOG.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
        noChange
      } { delayed =>
        val fx = delayed.info.action.toEffectPure
        val v2 = v0.withDelayed(
          v0.delayed - m.actionId
        )
        updated(v2, fx)
      }

  }

}

