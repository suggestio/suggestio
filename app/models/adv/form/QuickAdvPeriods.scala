package models.adv.form

import io.suggest.common.menum.EnumMaybeWithName
import org.joda.time.Period

/** Доступные интервалы размещения рекламных карточек. Отображаются в select'е вариантов adv-формы. */
object QuickAdvPeriods extends EnumMaybeWithName {

  /**
   * Класс элемента этого enum'а.
   * @param isoPeriod Строка iso-периода. Заодно является названием элемента. Заглавные буквы и цифры.
   */
  protected abstract class Val(val isoPeriod: String) extends super.Val(isoPeriod) {
    /** Приоритет при фильтрации. */
    def prio: Int
    def toPeriod = new Period(isoPeriod)
  }

  override type T = Val

  val P3D: T = new Val("P3D") {
    override def prio = 100
  }
  val P1W: T = new Val("P1W") {
    override def prio = 200
  }
  val P1M: T = new Val("P1M") {
    override def prio = 300
  }


  def default = P1W

  def ordered: List[T] = {
    values
      .foldLeft( List[T]() ) { (acc, e) => e :: acc }
      .sortBy(_.prio)
  }

}
