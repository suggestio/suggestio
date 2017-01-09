package io.suggest.dt

import io.suggest.dt.interval.{MRangeYmd, QuickAdvIsoPeriod, QuickAdvPeriod, QuickAdvPeriods}
import boopickle.Default._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 11:23
  * Description: Модель периода размещения.
  */
object MAdvPeriod {

  implicit val pickler: Pickler[MAdvPeriod] = {
    implicit val qapPickler = QuickAdvPeriod.pickler
    generatePickler[MAdvPeriod]
  }

  def toRange[Date_t](ap: MAdvPeriod)(implicit dtHelp: IDtHelper[Date_t]): MRangeYmd = {
    ap.quickAdvPeriod match {
      // Стандартный период. Считаем относительно now.
      case isoPeriod: QuickAdvIsoPeriod =>
        val start = dtHelp.now
        // учитывая, что результат now() **может быть mutable**, сразу считаем стартовый MYmd.
        val startYmd = dtHelp.toYmd(start)
        val end = isoPeriod.updateDate(start)
        val endYmd = dtHelp.toYmd(end)
        MRangeYmd(
          dateStart = startYmd,
          dateEnd = endYmd
        )

      // Кастомный период активен. Значит date range уже должен быть в соседнем поле.
      case _ =>
        ap.customRange.get
    }
  }

}


/**
  * Класс модели с инфой по периоду размещения.
  *
  * @param quickAdvPeriod Выбранный шаблон периода размещения.
  * @param customRange Список из 2 или 0 элементов, описывает кастомные даты начала и окончания размещения.
  */
case class MAdvPeriod(
  quickAdvPeriod  : QuickAdvPeriod        = QuickAdvPeriods.default,
  customRange     : Option[MRangeYmd]     = None
) {

  def withCustomRange(cr2: Option[MRangeYmd]) = copy(customRange = cr2)

}
