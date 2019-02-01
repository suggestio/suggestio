package io.suggest.sc.m.boot

import io.suggest.sc.m.ISc3Action

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.01.19 12:45
  * Description:
  */
sealed trait IBootAction extends ISc3Action


/** Сигнал к переходу но следующую стадию запуска. */
case class Boot( svcIds: List[MBootServiceId] = Nil ) extends IBootAction


/** Внутренний сигнал завершения boot-эффекта запуска. */
case class BootStartCompleted( svcId: MBootServiceId, tryRes: Try[_] ) extends IBootAction

