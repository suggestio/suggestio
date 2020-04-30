package io.suggest.log.buffered

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import io.suggest.log.ILogAppender
import io.suggest.sjs.dom2.DomQuick
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing

import scala.concurrent.duration._
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.04.2020 17:04
  * Description: Контроллер лог-буферизации.
  */
class BufLogAppendAh[M](
                         logAppenderUnd   : ILogAppender,
                         modelRW          : ModelRW[M, MBufAppendS],
                         dispatcher       : Dispatcher,
                       )
  extends ActionHandler( modelRW )
{

  private def EXPIRE_INTERVAL = 7.seconds


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Отправка в очередь лог-сообщений.
    case m: LogAppend =>
      val v0 = value

      if (m.logMsgs.isEmpty) {
        noChange

      } else {
        var modF = MBufAppendS.accRev.modify(m.logMsgs :: _)

        // Убедиться, что таймер запущен.
        val fxOpt = _resetAddExpireTimer( v0 )
        if (fxOpt.nonEmpty)
          modF = modF andThen MBufAppendS.expTimerId.modify( _.pending() )

        val v2 = modF( v0 )
        updatedSilent(v2)
      }


    // Запущен или сброшен таймер сброса логов.
    case m: ExpTimerUpdate =>
      val v0 = value

      val timerPot2 = m.timerId.fold(
        v0.expTimerId.fail,
        { timerIdOpt =>
          timerIdOpt.fold( Pot.empty[Int] )( v0.expTimerId.ready )
        }
      )

      val v2 = MBufAppendS.expTimerId.set( timerPot2 )(v0)
      updatedSilent(v2)


    // Срабатывание таймера сброса логов в бэкэнд.
    case ExpTimerAlarm =>
      val v0 = value
      val modF = MBufAppendS.expTimerId.set( Pot.empty )

      if (v0.accRev.exists(_.nonEmpty)) {
        val renderLogsFx = Effect.action {
          logAppenderUnd.logAppend(
            logMsgs = v0.accRev
              .reverseIterator
              .flatten
              .toSeq,
          )
          DoNothing
        }
        val v2 = (modF andThen MBufAppendS.accRev.set( Nil ))(v0)
        updatedSilent(v2, renderLogsFx)

      } else {
        // Почему-то сработал таймер.
        val v2 = modF( v0 )
        updatedSilent( v2 )
      }

  }


  /** Эффект перевыставления таймера. */
  private def _resetAddExpireTimer(v0: MBufAppendS): Option[Effect] = {
    Option.when( !v0.expTimerId.isPending ) {
      Effect.action {
        val timerIdTry = Try {
          v0.expTimerId
            .foreach( DomQuick.clearTimeout )
          val timerId = DomQuick.setTimeout( EXPIRE_INTERVAL.toMillis ) { () =>
            dispatcher( ExpTimerAlarm )
          }
          Some(timerId)
        }
        ExpTimerUpdate( timerIdTry )
      }
    }
  }

}
