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

  /** "2016.01.01" */
  def ymdToStringDot(ymd: MYmd) = ymdToString(ymd, ".")

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

  if (year < 2016 || year > 2222 || month < 1 || month > 12 || day < 1 || day > 31)
    throw new IllegalArgumentException( toString )

  override def toString: String = {
    MYmd.ymdToStringDot(this)
  }

}
