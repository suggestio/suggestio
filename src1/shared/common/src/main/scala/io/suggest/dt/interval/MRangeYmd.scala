package io.suggest.dt.interval

import boopickle.Default._
import io.suggest.dt.MYmd

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 12:25
  * Description: Интервал дат (ymd) с чёткими и определёнными краями.
  * Его стало не хватать, поэтому появился [[MRangeYmdOpt]].
  * Возможно, в будущем, этот range будет удалён.
  */
object MRangeYmd {

  /** Утиль для необратимого рендера диапазонов в строки. */
  object ToString {
    def PREFIX = "["
    def DELIM = ".."
    def SUFFIX = "]"
    def format(start: String, end: String): String = {
      PREFIX + start + DELIM + end + SUFFIX
    }
  }

  implicit val mRangeYmdPickler: Pickler[MRangeYmd] = {
    implicit val mymdP = MYmd.mYmdPickler
    generatePickler[MRangeYmd]
  }

}

case class MRangeYmd(dateStart: MYmd, dateEnd: MYmd) {

  override def toString: String = {
    MRangeYmd.ToString.format(dateStart.toString, end = dateEnd.toString)
  }

  def toSeq = dateStart :: dateEnd :: Nil

  def toRangeYmdOpt: MRangeYmdOpt = {
    MRangeYmdOpt(
      dateStartOpt = Some(dateStart),
      dateEndOpt   = Some(dateEnd)
    )
  }

}
