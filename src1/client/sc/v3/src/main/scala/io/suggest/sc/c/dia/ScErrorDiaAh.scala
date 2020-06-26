package io.suggest.sc.c.dia

import diode.{ActionHandler, ActionResult, Circuit, Effect, ModelRW}
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.m.{CheckRetryError, CloseError, MScRoot, OnlineCheckConn, RetryError, SetErrorState}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.2019 0:23
  * Description: Контроллер диалога ошибки.
  */
object ScErrorDiaAh extends Log {

  /** Сборка опционального эффекта отказа от подписки на события pot'а с ошибкой. */
  private def _maybeUnSubscribeFx(errDia0: MScErrorDia): Option[Effect] = {
    for {
      f <- errDia0.potUnSubscribe
    } yield {
      Effect.action {
        for (ex <- Try( f() ).failed)
          logger.warn( ErrorMsgs.EVENT_LISTENER_SUBSCRIBE_ERROR, ex, f )
        DoNothing
      }
    }
  }

}

class ScErrorDiaAh(
                    circuit: Circuit[MScRoot],
                    modelRW: ModelRW[MScRoot, Option[MScErrorDia]],
                  )
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[MScRoot]] = {

    case m: SetErrorState =>
      val v0 = value

      // Проверить связь с инетом
      val checkOnlineFx = OnlineCheckConn.toEffectPure

      if (v0.fold(true)(_.potRO.exists(_.value.isPending))) {
        // Если перезапись, то надо отписаться от любой предыдущей подписки на событие:
        var fx = checkOnlineFx

        // Надо подписаться на события изменения связанного pot'а, чтобы после error-диалог после retry мог сам скрыться с экрана.
        for (v <- v0; unSubsFx <- ScErrorDiaAh._maybeUnSubscribeFx(v))
          fx += unSubsFx

        val v2 = Some(m.scErr)
        updated( v2, fx )

      } else {
        // Две+ статические ошибки не выводим, просто оставляем всё как есть.
        effectOnly( checkOnlineFx )
      }


    // Запрошен повтор ошибочного экшена.
    case RetryError =>
      val v0 = value

      (for {
        errDia0 <- v0
        // retry-pending-ошибки можно перезаписывать новыми ошибками:
        if !errDia0.potIsPending
        retryAction <- errDia0.retryAction
      } yield {
        val retryFx = retryAction.toEffectPure + OnlineCheckConn.toEffectPure

        errDia0.potRO.fold {
          // Без pot для слежения - сразу скрыть диалог.
          val closeFx = CloseError.toEffectPure
          effectOnly( retryFx + closeFx )

        } { potModelRO =>
          // Есть zoom Pot -- подписаться на события, оставив диалог на экране.
          val unsubscribeF = circuit.subscribe( potModelRO ) { _ =>
            circuit.dispatch( CheckRetryError )
          }

          // (SNH) Если в состоянии есть старая unSubscribe-функция, то использовать её перед перезаписью:
          val oldUnSubscribeFxOpt = ScErrorDiaAh._maybeUnSubscribeFx( errDia0 )

          val errDia2 = (MScErrorDia.potUnSubscribe set Some(unsubscribeF))(errDia0)
          val allFx = (retryFx :: oldUnSubscribeFxOpt.toList)
            .mergeEffects
            .get

          updatedSilent( Some(errDia2), allFx )
        }
      })
        .getOrElse( noChange )


    // Сигнал о том, что retry-pot, подписанный на события, изменился. Нужно проверить, что там...
    case CheckRetryError =>
      (for {
        errDia0 <- value
        potZoom <- errDia0.potRO
        pot = potZoom.value
        if !pot.isPending && !pot.isFailed
      } yield {
        // Убрать диалог с экрана, т.к. pot завершился не ошибкой:
        effectOnly( CloseError.toEffectPure )
      })
        .getOrElse(noChange)


    // Закрыть диалог ошибки.
    case CloseError =>
      value.fold(noChange) { errDia0 =>
        var fx = OnlineCheckConn.toEffectPure
        for (subsFx <- ScErrorDiaAh._maybeUnSubscribeFx( errDia0 ))
          fx += subsFx

        updated( None, fx )
      }

  }

}
