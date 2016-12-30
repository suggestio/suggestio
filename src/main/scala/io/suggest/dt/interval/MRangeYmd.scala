package io.suggest.dt.interval

import boopickle.Default._
import io.suggest.dt.MYmd

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 12:25
  * Description: Интервал дат (ymd).
  */
object MRangeYmd {

  implicit val pickler: Pickler[MRangeYmd] = {
    implicit val mymdP = MYmd.pickler
    generatePickler[MRangeYmd]
  }

}

case class MRangeYmd(dateStart: MYmd, dateEnd: MYmd) {
  override def toString = "[" + dateStart + ".." + dateEnd + "]"
}
