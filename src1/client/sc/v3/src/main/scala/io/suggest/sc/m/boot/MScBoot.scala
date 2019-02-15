package io.suggest.sc.m.boot

import diode.FastEq
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.01.19 14:33
  * Description: Модель данных состояния запуска/инициализации системы.
  * Неявно-пустая модель, используется во время загрузки для разных задач загрузки.
  */
object MScBoot {

  def default = apply()

  /** Поддержка FastEq для инстансов [[MScBoot]]. */
  implicit object MScBootFastEq extends FastEq[MScBoot] {
    override def eqv(a: MScBoot, b: MScBoot): Boolean = {
      (a.services ===* b.services) &&
      (a.targets ===* b.targets) &&
      (a.wzFirstDone ===* b.wzFirstDone)
    }
  }

  @inline implicit def univEq: UnivEq[MScBoot] = UnivEq.derive[MScBoot]

  val services = GenLens[MScBoot](_.services)
  val targets  = GenLens[MScBoot](_.targets)
  val done     = GenLens[MScBoot](_.wzFirstDone)

}


/** Контейнер данных загрузки.
  *
  * @param services Массив данных о сервисах.
  * @param targets Текущие цели для запуска.
  */
case class MScBoot(
                    services          : Map[MBootServiceId, MBootServiceState]    = Map.empty,
                    targets           : Set[MBootServiceId]                       = Set.empty,
                    wzFirstDone       : Option[Boolean]                           = None,
                  )

