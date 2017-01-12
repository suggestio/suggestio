package io.suggest.dt

import com.google.inject.Inject
import io.suggest.dt.interval.QuickAdvIsoPeriod
import org.joda.time.{DateTime, Interval, Period}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.01.17 18:06
  * Description: Утиль для кое-какой работы с MAdvPeriod на стороне сервера.
  */
class MAdvPeriodJvm @Inject() (mRangeYmdJvm: MRangeYmdJvm) {

  /**
    * Приведение диапазона размещения к joda-time интервалу.
    * @param mAdvPeriod Период размещения.
    * @return joda Interval.
    */
  def toJodaInterval(mAdvPeriod: MAdvPeriod): Interval = {
    mAdvPeriod.quickAdvPeriod match {
      case qapIso: QuickAdvIsoPeriod =>
        val p = new Period( qapIso.isoPeriod )
        val dateStart = new DateTime()
        val dateEnd = dateStart.plus(p).minusDays(1)
        new Interval(dateStart, dateEnd)

      case _ =>
        val dtPeriod = mAdvPeriod.customRange.get
        mRangeYmdJvm.toJodaInterval(dtPeriod)
    }
  }

}
