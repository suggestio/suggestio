package io.suggest.dt.interval

import io.suggest.common.menum.{ILightEnumeration, StrIdValT}

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


  /** Префикс кода локализованного названия периода (i18n). */
  def MESSAGES_PREFIX = "adv.period."

}

trait QuickAdvPeriodsT extends ILightEnumeration with StrIdValT {

  /** Интерфейс для элемента модели. */
  protected[this] trait ValT extends super.ValT {

    def messagesCode = PeriodsConstants.MESSAGES_PREFIX + strId

  }

  override type T <: ValT

  /** 3 дня. */
  val P3D: T

  /** 1 неделя. */
  val P1W: T

  /** 1 месяц. */
  val P1M: T


  def default: T = P3D

}
