package io.suggest.dt.moment

import com.momentjs.{Moment, Units}
import io.suggest.dt.{IYmdHelper, Month0Indexed}

import java.time.LocalDate
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 11:06
  * Description: Внутренняя утиль для Moment.js.
  */
object MomentJsUtil {


  /** Самая implicit-утиль из moment-утили. */
  object Implicits {

    implicit class MomentUtil(val m: Moment) extends AnyVal {
      def tomorrow: Moment = plusDays(1)
      def plusDays(days: Int) = m.add(days, Units.day)
      def plusYears(years: Int) = m.add(years, Units.year)
    }


    /** Реализация IDtHelper для moment-инстансов. */
    implicit object MomentDateExt extends Month0Indexed[Moment] {

      override def now: Moment = Moment()

      override def plusDays(date: Moment, days: Int): Moment = {
        date.add(days, Units.day)
      }

      override def plusMonths(date: Moment, months: Int): Moment = {
        date.add(months, Units.month)
      }

      override def yearDmonthDay2date(year: Int, dateMonth: Int, day: Int): Moment = {
        Moment( js.Array(year, dateMonth, day) )
      }

      override def getDateMonthOfYear(date: Moment): Int = {
        date.month()
      }

      override def getYear(date: Moment): Int = {
        date.year()
      }

      override def getDayOfMonth(date: Moment): Int = {
        date.date()
      }

    }


    implicit final class MomentOpsExt( private val moment: Moment ) extends AnyVal {
      def toLocalDate: LocalDate = {
        implicitly[IYmdHelper[Moment]]
          .toYmd( moment )
          .to[LocalDate]
      }
    }


    implicit final class JLocalDateMomentExt( private val localDate: LocalDate ) extends AnyVal {

      def toMoment: Moment = {
        implicitly[IYmdHelper[LocalDate]]
          .toYmd( localDate )
          .to[Moment]
      }

    }


  }

}
