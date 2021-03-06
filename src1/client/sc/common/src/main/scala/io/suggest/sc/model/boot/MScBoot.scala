package io.suggest.sc.model.boot

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
      (a.targets ===* b.targets)
    }
  }

  @inline implicit def univEq: UnivEq[MScBoot] = UnivEq.derive[MScBoot]

  def services        = GenLens[MScBoot](_.services)
  def targets         = GenLens[MScBoot](_.targets)


  implicit final class ScBootExt( private val scBoot: MScBoot ) extends AnyVal {

    /** At least one services completed boot procedure (no matter - failed or not). */
    def isBootCompleted: Boolean =
      scBoot.targets.isEmpty && scBoot.services.nonEmpty

  }

}


/** Контейнер данных загрузки.
  *
  * @param services Массив данных о сервисах.
  * @param targets Текущие цели для запуска.
  */
case class MScBoot(
                    services          : Map[MBootServiceId, MBootServiceState]    = Map.empty,
                    targets           : Set[MBootServiceId]                       = Set.empty,
                  )

