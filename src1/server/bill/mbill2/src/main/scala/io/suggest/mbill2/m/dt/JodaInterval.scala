package io.suggest.mbill2.m.dt

import org.joda.time.{Interval, DateTime}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.12.15 9:59
 * Description: Объект-компаньон для joda Interval.
 */
object JodaInterval {

  def apply(start: DateTime, end: DateTime): Interval = {
    new Interval(start, end)
  }

  def applyOpt(startOpt: Option[DateTime], endOpt: Option[DateTime]): Option[Interval] = {
    for (start <- startOpt; end <- endOpt) yield {
      apply(start, end = end)
    }
  }

  def tupledOpt = (applyOpt _).tupled

  def unapply(i: Interval): Option[(DateTime, DateTime)] = {
    Some((i.getStart, i.getEnd))
  }

  def unapplyOpt(intOpt: Option[Interval]): Option[(Option[DateTime], Option[DateTime])] = {
    for (int <- intOpt) yield {
      (Some(int.getStart), Some(int.getEnd))
    }
  }

}
