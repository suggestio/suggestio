package io.suggest.dt.interval

import boopickle.Default._
import io.suggest.common.empty.EmptyProduct
import io.suggest.dt.{IYmdHelper, MYmd}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.01.17 21:14
  * Description: Кросс-платформенный диапазон дат с опциональными краями.
  */
object MRangeYmdOpt {

  implicit val mRangeYmdOptPickler: Pickler[MRangeYmdOpt] = {
    implicit val mYmdP = MYmd.mYmdPickler
    generatePickler[MRangeYmdOpt]
  }

  def applyFrom[T](dateStartOpt: Option[T], dateEndOpt: Option[T])(implicit ev: IYmdHelper[T]): MRangeYmdOpt = {
    val f = ev.toYmd _
    MRangeYmdOpt(
      dateStartOpt = dateStartOpt.map(f),
      dateEndOpt   = dateEndOpt.map(f)
    )
  }

  def empty = MRangeYmdOpt()

  @inline implicit def univEq: UnivEq[MRangeYmdOpt] = UnivEq.derive

}

case class MRangeYmdOpt(
                         dateStartOpt: Option[MYmd] = None,
                         dateEndOpt  : Option[MYmd] = None
                       )
  extends EmptyProduct
{

  override def toString: String = {
    MRangeYmd.ToString.format(
      start = MYmd.toStringOpt(dateStartOpt),
      end   = MYmd.toStringOpt(dateEndOpt)
    )
  }

  def toRangeYmdOption: Option[MRangeYmd] = {
    for {
      dateStart <- dateStartOpt
      dateEnd   <- dateEndOpt
    } yield {
      MRangeYmd(dateStart, dateEnd)
    }
  }

}
