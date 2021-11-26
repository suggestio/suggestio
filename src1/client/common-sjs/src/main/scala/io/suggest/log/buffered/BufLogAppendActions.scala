package io.suggest.log.buffered

import io.suggest.log.{ILogAction, MLogMsg}

import scala.scalajs.js.timers.SetTimeoutHandle
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.04.2020 13:49
  * Description: Экшены буферизатора логов.
  */
sealed trait IBufLogAppendAction extends ILogAction

/** Закидывание лог-сообщений в буфер. */
case class LogAppend( logMsgs: Seq[MLogMsg] ) extends IBufLogAppendAction

/** Результат обновления таймера. */
case class ExpTimerUpdate( timerId: Try[Option[SetTimeoutHandle]] ) extends IBufLogAppendAction

/** Срабатывание таймера. */
case object ExpTimerAlarm extends IBufLogAppendAction

