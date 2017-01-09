package io.suggest.sjs.common.dt

import diode.FastEq
import io.suggest.dt.interval.MRangeYmd
import io.suggest.dt.{IDtHelper, MYmd}

import scala.scalajs.js.Date

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 13:21
  * Description: Вспомогательная утиль вокруг js.Date.
  */
object JsDateUtil {

  /** Расширения для js.Date. */
  implicit class JsDateExt(val jsDate0: Date) extends AnyVal {

    /** Добавить дней в дату, вернув обновлённую дату.
      * @see [[http://stackoverflow.com/a/563442]]
      */
    def plusDays(days: Int): Date = {
      jsDate0.setDate(jsDate0.getDate() + days)
      jsDate0
    }

    def plusMonths(months: Int): Date = {
      jsDate0.setMonth( jsDate0.getMonth() + months )
      jsDate0
    }

  }


  /** typeclass для поддержки датовой арифметики на базе js.Date. */
  implicit object JsDateHelper extends IDtHelper[Date] {

    override def now: Date = new Date()

    override def plusDays(date: Date, days: Int): Date = {
      date.plusDays(days)
    }

    override def plusMonths(date: Date, months: Int): Date = {
      date.plusMonths(months)
    }

    override def fromYmd(ymd: MYmd): Date = {
      new Date(
        year  = ymd.year,
        month = month12ToPlatformMonth(ymd.month),
        date  = ymd.day
      )
    }

    override def month12ToPlatformMonth(month: Int): Int = {
      month - 1
    }

    override def toYmd(date: Date): MYmd = {
      MYmd(
        year  = getYear(date),
        month = getMonthOfYear(date),
        day   = getDayOfMonth(date)
      )
    }

    override def getMonthOfYear(date: Date): Int = {
      date.getMonth() + 1
    }

    override def getYear(date: Date): Int = {
      date.getFullYear()
    }

    override def getDayOfMonth(date: Date): Int = {
      date.getDate()
    }

  }


  /** Реализация FastEq для модели MRangeYmd. */
  implicit object MRangeYmdFastEq extends FastEq[MRangeYmd] {
    override def eqv(a: MRangeYmd, b: MRangeYmd): Boolean = {
      (a.dateStart eq b.dateStart) && (a.dateEnd eq b.dateEnd)
    }
  }

}
