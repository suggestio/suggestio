package io.suggest.sc.m.boot

import diode.FastEq
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.01.19 14:33
  * Description: Модель данных состояния запуска/инициализации системы.
  */
object MScBoot {

  def default = apply()

  /** Поддержка FastEq для инстансов [[MScBoot]]. */
  implicit object MScBootFastEq extends FastEq[MScBoot] {
    override def eqv(a: MScBoot, b: MScBoot): Boolean = {
      (a.services ===* b.services) &&
      (a.targets ===* b.targets)
    }
  }

  @inline implicit def univEq: UnivEq[MScBoot] = UnivEq.derive[MScBoot]

}


/** Контейнер данных загрузки.
  *
  * @param services Массив данных о сервисах.
  * @param targets Текущие цели для запуска.
  */
case class MScBoot(
                    services          : Map[MBootServiceId, MBootServiceState]    = Map.empty,
                    targets           : Set[MBootServiceId]                       = Set.empty,
                  ) {

  def withServices( services: Map[MBootServiceId, MBootServiceState] ) = copy(services = services)
  def withTargets( targets: Set[MBootServiceId] ) = copy(targets = targets)

}

