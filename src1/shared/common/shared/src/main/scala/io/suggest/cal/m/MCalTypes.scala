package io.suggest.cal.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 12:42
  * Description: Модель типов календарей. Вынесена из www models.mcal.MCalTypes, где она жила поверх scala.Enumeration.
  */
object MCalTypes extends StringEnum[MCalType] {

  /** Будни. */
  case object WeekDay extends MCalType("d") {
    override def i18nCode = "Week.days"
    override def dayStart = Some(1)
    override def dayEnd   = Some(5)
  }

  /** Выходные. */
  case object WeekEnd extends MCalType("e") {
    override def i18nCode     = "Weekend"
    override def dayStart = Some(6)
    override def dayEnd   = Some(7)

    override def maybeWeekend(dow: Int, weekEndDays: Set[Int]): Boolean = {
      weekEndDays.contains(dow)
    }
  }

  /** Прайм-тайм шоппинга. */
  case object PrimeTime extends MCalType("p") {
    override def i18nCode     = "Holidays.primetime"
    override def dayStart = None
    override def dayEnd   = None
  }

  /** Все дни. Т.е. календарь на всю неделю. */
  case object All extends MCalType("a") {
    override def i18nCode     = "All.week"
    override def dayStart = Some(1)
    override def dayEnd   = Some(7)
  }


  def default = All

  override def values = findValues

}


/** Класс одного элемнта модели, описывает тип календаря. */
sealed abstract class MCalType(override val value: String) extends StringEnumEntry {

  /** messages-код отображаемого названия календаря. */
  def i18nCode: String

  /** Опциональное код дня начала периода. */
  def dayStart: Option[Int]

  /** Опциональное код дня окончания периода. */
  def dayEnd: Option[Int]

  /** Костыль к jollyday для weekend-календарей, которые не могут описывать все выходные в году как праздники. */
  def maybeWeekend(dow: Int, weekEndDays: Set[Int]): Boolean = false

  override final def toString = value

}


object MCalType {

  implicit def mCalTypeFormat: Format[MCalType] = {
    EnumeratumUtil.valueEnumEntryFormat( MCalTypes )
  }

  @inline implicit def univEq: UnivEq[MCalType] = UnivEq.derive

}
