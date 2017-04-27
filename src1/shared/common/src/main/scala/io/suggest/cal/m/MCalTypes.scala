package io.suggest.cal.m

import boopickle.Default._
import enumeratum._
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 12:42
  * Description: Модель типов календарей. Вынесена из www models.mcal.MCalTypes, где она жила поверх scala.Enumeration.
  */
object MCalType {

  /** Поддержка бинарной сериализации.*/
  implicit val mCalTypePickler: Pickler[MCalType] = {
    compositePickler[MCalType]
      .addConcreteType[MCalTypes.WeekDay.type]
      .addConcreteType[MCalTypes.WeekEnd.type]
      .addConcreteType[MCalTypes.PrimeTime.type]
      .addConcreteType[MCalTypes.All.type]
  }

}

/** Класс одного элемнта модели, описывает тип календаря. */
sealed abstract class MCalType extends EnumEntry with IStrId {

  /** messages-код отображаемого названия календаря. */
  def name: String

  /** Опциональное код дня начала периода. */
  def dayStart: Option[Int]

  /** Опциональное код дня окончания периода. */
  def dayEnd: Option[Int]

  /** Костыль к jollyday для weekend-календарей, которые не могут описывать все выходные в году как праздники. */
  def maybeWeekend(dow: Int, weekEndDays: Set[Int]): Boolean = false

  override def toString = strId

}


/** Статическая модель типов календарей. */
object MCalTypes extends Enum[MCalType] {

  /** Будни. */
  case object WeekDay extends MCalType {
    override def name     = "Week.days"
    override def dayStart = Some(1)
    override def dayEnd   = Some(5)
    override def strId    = "d"
    override def toString = super.toString
  }

  /** Выходные. */
  case object WeekEnd extends MCalType {
    override def strId    = "e"
    override def name     = "Weekend"
    override def dayStart = Some(6)
    override def dayEnd   = Some(7)

    override def maybeWeekend(dow: Int, weekEndDays: Set[Int]): Boolean = {
      weekEndDays.contains(dow)
    }
    override def toString = super.toString
  }

  /** Прайм-тайм шоппинга. */
  case object PrimeTime extends MCalType {
    override def strId    = "p"
    override def name     = "Holidays.primetime"
    override def dayStart = None
    override def dayEnd   = None
    override def toString = super.toString
  }

  /** Все дни. Т.е. календарь на всю неделю. */
  case object All extends MCalType {
    override def strId    = "a"
    override def name     = "All.week"
    override def dayStart = Some(1)
    override def dayEnd   = Some(7)
    override def toString = super.toString
  }


  def default = All

  override lazy val values = findValues

}
