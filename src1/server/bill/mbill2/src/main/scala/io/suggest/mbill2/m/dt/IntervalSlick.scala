package io.suggest.mbill2.m.dt

import org.joda.time.Interval

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.12.15 9:56
 * Description: slick-поддержка временнОго интервала, т.е. две даты от-до.
 */
trait IntervalSlick
  extends DateStartSlick
  with DateEndSlick
{

  import driver.api._

  /** Поддержка отмапленного временного интервала. */
  trait IntervalColumn
    extends DateStartOpt
    with DateEndOpt
  { that: Table[_] =>

    def dtIntervalOpt = (dateStartOpt, dateEndOpt) <> (
      JodaInterval.tupledOpt, JodaInterval.unapplyOpt
    )

  }

}

trait IDtIntervalOpt {
  def dtIntervalOpt: Option[Interval]
}
