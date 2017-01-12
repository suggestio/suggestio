package io.suggest.dt

import com.google.inject.Inject
import io.suggest.dt.interval.MRangeYmd
import org.joda.time.Interval

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.12.16 11:22
  * Description: Серверная утиль для работы с кросс-платформенной моделью MRangeYmd.
  */
class MRangeYmdJvm @Inject() (mYmdJvm: MYmdJvm) {

  def apply(ivl: Interval): MRangeYmd = {
    MRangeYmd(
      dateStart = mYmdJvm(ivl.getStart),
      dateEnd   = mYmdJvm(ivl.getEnd)
    )
  }


  def toJodaInterval(rymd: MRangeYmd): Interval = {
    new Interval(
      mYmdJvm.toJodaDateTime(rymd.dateStart),
      mYmdJvm.toJodaDateTime(rymd.dateEnd)
    )
  }

}
