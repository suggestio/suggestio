package io.suggest.dt

import boopickle.Default._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.12.16 15:27
  * Description: Сериализуемая легковесная модель даты.
  * Изначально задумывалась для задания дат размещения (adv-geo).
  */
object MYmd {

  implicit val pickler: Pickler[MYmd] = generatePickler[MYmd]

  /** Отрендерить в строку, используя указанный разделитель. */
  def ymdToString(ymd: MYmd, sep: String): String = {
    val fmt = "%02d"
    val m = fmt.format(ymd.month)
    val d = fmt.format(ymd.day)
    ymd.year.toString + sep + m + sep + d
  }

  /**
    * "2016-01-01" стандартный человеческий формат, понимаемый браузерами.
    * Точки в качестве разделителей не понимаются.
    */
  def ymdToStringStd(ymd: MYmd) = ymdToString(ymd, "-")

  implicit object MYmdOrd extends Ordering[MYmd] {
    override def compare(x: MYmd, y: MYmd): Int = {
      if (x < y) -1 else if (x > y) 1 else 0
    }
  }

}


/**
  * Класс модели даты.
  * @param year Год.
  * @param month Месяц.
  * @param day День.
  */
case class MYmd(
  year  : Int,
  month : Int,
  day   : Int
) {

  override def toString: String = {
    MYmd.ymdToStringStd(this)
  }

}
