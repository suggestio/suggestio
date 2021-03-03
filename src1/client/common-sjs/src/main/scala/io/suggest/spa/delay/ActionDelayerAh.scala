package io.suggest.spa.delay

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 17:26
  * Description: Контроллер управления отложенными экшенами, которые не должны запускаться чаще указанного времени.
  * Просто отложить экшен/эффект - не проблема: Effect().after(...).
  * Однако, если нужно лимитировать частоту запуска эффекта, то этот контроллер облегчает задачу:
  * задать ключ, передать эффект и время. По таймауту будет запущен только самый последний эффект с данным ключом.
  */
class ActionDelayerAh[M](
                          modelRW: ModelRW[M, MDelayerS],
                        )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Команда к откладывания экшена для повторного исполнения через какое-то время.
    case m: DelayAction =>
      // Организуем откладывание экшена на потом:
      val v0 = value
      val fx = Effect {
        DomQuick
          .timeoutPromiseT(m.delayMs)( FireDelayedAction(m.key) )
          .fut
      }

      // Сохраняем итоги деятельности в состояние.
      val a2 = MDelayedAction( m )
      val v2 = MDelayerS.delayed
        .modify(_.updated(m.key, a2))(v0)
      updated(v2, fx)


    // Настала пора исполнить отложенный на потом экшен:
    case m: FireDelayedAction =>
      val v0 = value
      (for {
        delayed <- v0.delayed.get( m.key )
      } yield {
        val fx = delayed.info.fx
        val v2 = MDelayerS.delayed
          .modify( _ - m.key )(v0)

        updated(v2, fx)
      })
        .getOrElse {
          logger.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = m )
          noChange
        }

  }

}

