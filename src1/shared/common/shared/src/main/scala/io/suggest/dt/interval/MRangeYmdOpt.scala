package io.suggest.dt.interval

import io.suggest.common.empty.EmptyProduct
import io.suggest.dt.{IYmdHelper, MYmd}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.01.17 21:14
  * Description: Кросс-платформенный диапазон дат с опциональными краями.
  */
object MRangeYmdOpt {

  def applyFrom[T](dateStartOpt: Option[T], dateEndOpt: Option[T])(implicit ev: IYmdHelper[T]): MRangeYmdOpt = {
    val f = ev.toYmd _
    MRangeYmdOpt(
      dateStartOpt = dateStartOpt.map(f),
      dateEndOpt   = dateEndOpt.map(f)
    )
  }

  def empty = MRangeYmdOpt()

  implicit def rangeYmdOptJson: OFormat[MRangeYmdOpt] = (
    (__ \ "s").formatNullable[MYmd] and
    (__ \ "e").formatNullable[MYmd]
  )(apply, unlift(unapply))


  implicit final class RangeYmdOptExt( private val rangeYmdOpt: MRangeYmdOpt ) extends AnyVal {

    def toRangeYmdOption: Option[MRangeYmd] = {
      for {
        dateStart <- rangeYmdOpt.dateStartOpt
        dateEnd   <- rangeYmdOpt.dateEndOpt
      } yield {
        MRangeYmd(dateStart, dateEnd)
      }
    }

  }

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

}
