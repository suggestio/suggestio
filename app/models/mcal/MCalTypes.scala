package models.mcal

import io.suggest.common.empty.EmptyUtil
import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsT
import play.api.data.Mapping

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.16 15:29
  * Description: Статическая модель "типов" календарей, т.е. статических локализаций оных.
  * Календарей может быть много, а локализации и прочее -- ограниченный набор.
  */
object MCalTypes extends EnumMaybeWithName with EnumJsonReadsT {

  /** Класс экземпляров модели. */
  protected[this] abstract class Val(val strId: String) extends super.Val(strId) {

    /** messages-код отображаемого названия календаря. */
    def name: String

    /** Опциональное код дня начала периода. */
    def dayStart: Option[String]

    /** Опциональное код дня окончания периода. */
    def dayEnd: Option[String]

    /** Костыль к jollyday для weekend-календарей, которые не могут описывать все выходные в году как праздники. */
    def maybeWeekend(dow: Int, weekEndDays: Set[Int]): Boolean = false

  }

  override type T = Val

  /** Будни. */
  val WeekDay: T = new Val("d") {
    override def name     = "Week.days"
    override def dayStart = Some("mo")
    override def dayEnd   = Some("th")
  }

  /** Выходные. */
  val WeekEnd: T = new Val("e") {
    override def name     = "Weekend"
    override def dayStart = Some("fr")
    override def dayEnd   = Some("su")

    override def maybeWeekend(dow: Int, weekEndDays: Set[Int]): Boolean = {
      weekEndDays.contains(dow)
    }
  }

  /** Прайм-тайм шоппинга. */
  val PrimeTime: T = new Val("p") {
    override def name     = "Holidays.primetime"
    override def dayStart = None
    override def dayEnd   = None
  }

  /** Все дни. Т.е. календарь на всю неделю. */
  val All: T = new Val("a") {
    override def name     = "All.week"
    override def dayStart = Some("mo")
    override def dayEnd   = Some("su")
  }


  def default: T = All

  import play.api.data.Forms._

  /** Опциональный маппинг для формы. */
  def calTypeOptM: Mapping[Option[MCalType]] = {
    nonEmptyText(minLength = 1, maxLength = 10)
      .transform [Option[MCalType]] (maybeWithName, _.fold("")(_.strId))
  }

  /** Обязательный маппинг для формы. */
  def calTypeM: Mapping[MCalType] = {
    calTypeOptM
      .verifying("error.required", _.nonEmpty)
      .transform [MCalType] (EmptyUtil.getF, EmptyUtil.someF)
  }

}
