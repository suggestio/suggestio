package io.suggest.sc.m.boot

import io.suggest.sc.m.ISc3Action

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.01.19 12:45
  * Description: Экшены для контроллера [[io.suggest.sc.c.boot.BootAh]].
  */
sealed trait IBootAction extends ISc3Action


/** Сигнал к переходу но следующую стадию запуска. */
case class Boot( svcIds: List[MBootServiceId] = Nil ) extends IBootAction


/** Внутренний сигнал завершения boot-эффекта запуска. */
case class BootStartCompleted( svcId: MBootServiceId, tryRes: Try[_] ) extends IBootAction


/** Экшен запуска этапа подготовки данных геолокации. */
case object BootLocDataWz extends IBootAction

/** Экшен после запуска инициализации визарда. */
case object BootLocDataWzAfterInit extends IBootAction

/** Экшен реакции на завершение действий визарда первого запуска. */
case class BootLocDataWzAfterWz(startedAtMs: Long) extends IBootAction
