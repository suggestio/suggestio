package models.mcal

import de.jollyday.HolidayManager

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 16:41
 * Description: bill v1 контекст с календарями.
 */
trait ICalsCtx {

  /** Карта доступных для работы календарей.. */
  def calsMap: Map[String, ICalCtx]

  override def toString: String = {
    s"${getClass.getSimpleName}(${calsMap.size}cals)"
  }

}


object MCalsCtx {
  def empty = MCalsCtx( Map.empty )
}

/** Дефолтовая реализация модели контекстов календарей [[ICalsCtx]]. */
case class MCalsCtx(
  override val calsMap: Map[String, MCalCtx]
)
  extends ICalsCtx
{
  override def toString = super.toString
}

/** Контекст одного календаря. */
trait ICalCtx {

  // TODO желательно бы сюда пихать инстанс MCalendar.

  /** id календаря. */
  def calId: String

  /** Экземпляр календаря. */
  def mcal: MCalendar

  /** Менеджер календаря праздников. */
  def mgr: HolidayManager

}


/** Дефолтовая реализация модели контекста одного календаря [[ICalCtx]]. */
case class MCalCtx(
  override val calId  : String,
  override val mcal   : MCalendar,
  override val mgr    : HolidayManager
)
  extends ICalCtx
