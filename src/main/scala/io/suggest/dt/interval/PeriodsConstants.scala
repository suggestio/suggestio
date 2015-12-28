package io.suggest.dt.interval

import io.suggest.common.menum.ILightEnumeration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 14:37
 * Description: Заготовка модели быстрых периодов.
 */
object PeriodsConstants {

  def P_3DAYS   = "P3D"

  def P_1WEEK   = "P1W"

  def P_1MONTH  = "P1M"

  def CUSTOM    = "custom"

}

trait QuickAdvPeriodsT extends ILightEnumeration {

  /** Интерфейс для элемента модели. */
  protected[this] trait ValT extends super.ValT {

    /** Строковое значение ISO3231-периода. */
    def isoPeriod: String

  }

  override type T <: ValT

  /** 3 дня. */
  val P3D: T

  /** 1 неделя. */
  val P1W: T

  /** 1 месяц. */
  val P1M: T

}
