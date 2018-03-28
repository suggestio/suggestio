package io.suggest.dt

import boopickle.Default._
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.12.16 15:27
  * Description: Сериализуемая легковесная модель даты.
  * Изначально задумывалась для задания дат размещения (adv-geo).
  */
object MYmd {

  implicit val mYmdPickler: Pickler[MYmd] = generatePickler[MYmd]

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
    * TODO Это кажется уже не актуально, можно будет точку вернуть назад.
    */
  def ymdToStringStd(ymd: MYmd) = ymdToString(ymd, "-")

  implicit object MYmdOrd extends Ordering[MYmd] {
    override def compare(x: MYmd, y: MYmd): Int = {
      x.sortKey - y.sortKey
    }
  }

  def from[T](date: T)(implicit ev: IYmdHelper[T]): MYmd = {
    ev.toYmd(date)
  }

  def toStringOpt(ymdOpt: Option[MYmd]): String = {
    ymdOpt.fold("")(_.toString)
  }

  /** Поддержка play-json. */
  implicit def mYmdFormat: Format[MYmd] = (
    (__ \ "y").format[Int] and
    (__ \ "m").format[Int] and
    (__ \ "d").format[Int]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MYmd] = UnivEq.derive

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

  /** Целочисленный ключ для сортировки. */
  def sortKey = (year - 2016) * 365 + month * 12 + day

  def to[T](implicit ev: IYmdHelper[T]): T = {
    ev.toDate(this)
  }

}
