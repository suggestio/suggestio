package models.mdt

import java.time.LocalDate

import io.suggest.dt.{IPeriodInfo, IYmdHelper}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.16 18:47
 * Description: Интерфейс для моделей, описывающих период из двух дат.
 */

trait IDateStartEnd {

  /** Дата начала. */
  def dateStart: LocalDate
  def dtStart = dateStart.atStartOfDay()


  /** Дата окончания. */
  def dateEnd: LocalDate
  def dtEnd = dateEnd.atStartOfDay()

  override def toString: String = s"$dateStart..$dateEnd"
}


object MDateStartEnd {

  def apply(m: IPeriodInfo)(implicit ymdHelper: IYmdHelper[LocalDate]): MDateStartEnd = {
    MDateStartEnd(
      dateStart = m.dateStart[LocalDate],
      dateEnd   = m.dateEnd[LocalDate]
    )
  }

}

/** Дефолтовая реализация [[IDateStartEnd]]. */
case class MDateStartEnd(override val dateStart : LocalDate,
                         override val dateEnd   : LocalDate)
  extends IDateStartEnd
{

  override def toString = super.toString

}
