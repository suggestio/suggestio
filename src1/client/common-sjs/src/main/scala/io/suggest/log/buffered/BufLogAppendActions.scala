package io.suggest.log.buffered

import io.suggest.log.MLogMsg
import io.suggest.spa.DAction

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.04.2020 13:49
  * Description: Экшены буферизатора логов.
  */
sealed trait IBufLogAppendAction extends DAction

/** Закидывание лог-сообщений в буфер. */
case class LogAppend( logMsgs: Seq[MLogMsg] ) extends IBufLogAppendAction

/** Результат обновления таймера. */
case class ExpTimerUpdate( timerId: Try[Option[Int]] ) extends IBufLogAppendAction

/** Срабатывание таймера. */
case object ExpTimerAlarm extends IBufLogAppendAction

