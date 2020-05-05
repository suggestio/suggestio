package io.suggest.log.filter

import io.suggest.log.{ILogAppender, LogSeverity, MLogMsg}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.05.2020 12:16
  * Description: Фильтрация по severity.
  */
class SevereFilter(
                    minSeverity     : LogSeverity,
                    underlying      : ILogAppender,
                  )
  extends ILogAppender
{

  override def logAppend( logMsgs: Seq[MLogMsg] ): Unit = {
    val logMsgs2 = logMsgs.filter( _.severity.value >= minSeverity.value )
    if (logMsgs2.nonEmpty)
      underlying.logAppend( logMsgs2 )
  }

}
