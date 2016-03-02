package models.adv.form

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.dt.interval.PeriodsConstants._
import io.suggest.dt.interval.QuickAdvPeriodsT
import org.joda.time.Period

/** Доступные интервалы размещения рекламных карточек. Отображаются в select'е вариантов adv-формы. */
object QuickAdvPeriods extends EnumMaybeWithName with QuickAdvPeriodsT {

  /**
   * Класс элемента этого enum'а.
   * @param isoPeriod Строка iso-периода. Заодно является названием элемента. Заглавные буквы и цифры.
   */
  protected class Val(override val isoPeriod: String)
    extends super.Val(isoPeriod)
    with super.ValT
  {
    def toPeriod = new Period(isoPeriod)
  }

  override type T = Val

  override val P3D: T = new Val(P_3DAYS)
  override val P1W: T = new Val(P_1WEEK)
  override val P1M: T = new Val(P_1MONTH)


  def default = P3D

  def ordered: Iterable[T] = {
    valuesT
  }

}
