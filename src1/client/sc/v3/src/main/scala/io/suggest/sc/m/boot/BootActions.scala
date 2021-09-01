package io.suggest.sc.m.boot

import diode.Effect
import io.suggest.sc.c.in.BootAh
import io.suggest.sc.m.ISc3Action

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.01.19 12:45
  * Description: Экшены для контроллера [[BootAh]].
  */
sealed trait IBootAction extends ISc3Action


/** Сигнал к переходу но следующую стадию запуска. */
case class Boot( svcIds: List[MBootServiceId] = Nil ) extends IBootAction

/** Добавление эффекта после окончания загрузки указанного сервиса.
  * Если сервис уже загружен, то эффект будет запущен сразу.
  * Если сервис отсутствует в планах по загрузке, то будет использован ifMissing,
  * который может содержать в себе связку Boot(svc) >> BootAfter(svc, ...) или что-то ещё.
  */
case class BootAfter( svcId: MBootServiceId, fx: Effect, ifMissing: Option[Effect] = None ) extends IBootAction


/** Внутренний сигнал завершения boot-эффекта запуска. */
case class BootStartCompleted( svcId: MBootServiceId, tryRes: Try[_] ) extends IBootAction
