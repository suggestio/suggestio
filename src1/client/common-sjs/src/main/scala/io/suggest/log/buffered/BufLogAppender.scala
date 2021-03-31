package io.suggest.log.buffered

import diode.Circuit
import io.suggest.log.{ILogAppender, MLogMsg}
import io.suggest.primo.Keep
import io.suggest.sjs.common.view.CommonPage
import io.suggest.spa.DoNothingActionProcessor

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.04.2020 15:24
  * Description: Поддержка буферизации для лог-аппендера.
  */
final class BufLogAppender(
                            underlying: ILogAppender
                          )
  extends Circuit[MBufAppendS]
  with ILogAppender
{

  override protected def initialModel = MBufAppendS()

  override protected val actionHandler: HandlerFunction = {
    new BufLogAppendAh(
      modelRW         = zoomRW(identity)(Keep.right),
      underlying      = underlying,
      dispatcher      = this,
    )
  }

  override def logAppend(logMsgs: Seq[MLogMsg]): Unit =
    dispatch( LogAppend(logMsgs) )


  // Подписаться на window.onbeforeunload
  Try {
    CommonPage.onClose( () => dispatch(ExpTimerAlarm) )
  }


  addProcessor( DoNothingActionProcessor[MBufAppendS] )

}

