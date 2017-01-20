package io.suggest.dt

import java.time.{LocalDate => Java8LocalDate}

import com.google.inject.Singleton
import org.joda.time.{DateTime => JodaDateTime, LocalDate => JodaLocalDate}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 15:56
  * Description: Инжектируемая либа с утилью для связывания кроссплатфроменного MYmd
  * с jvm-платформенными представлениями дат.
  */
@Singleton
class YmdHelpersJvm {

  /** Неявное API упаковано в отдельный object. */
  object Implicits {

    /** Поддержка связывания Joda-time и MYmd. */
    implicit object Java8LocalDateYmdHelper extends IYmdHelper[Java8LocalDate] {

      override def now: Java8LocalDate = Java8LocalDate.now()

      override def plusDays(date: Java8LocalDate, days: Int): Java8LocalDate = {
        date.plusDays(days)
      }

      override def plusMonths(date: Java8LocalDate, months: Int): Java8LocalDate = {
        date.plusMonths(months)
      }

      override def yearDmonthDay2date(year: Int, dateMonth: Int, day: Int): Java8LocalDate = {
        Java8LocalDate.of(year, dateMonth, day)
      }

      override def getDateMonthOfYear(date: Java8LocalDate): Int = {
        date.getMonthValue
      }

      override def getYear(date: Java8LocalDate): Int = {
        date.getYear
      }

      override def getDayOfMonth(date: Java8LocalDate): Int = {
        date.getDayOfMonth
      }

      /** Java-8 time использует человеческую нумерацию месяцев. Инопланетного сдвига нет. */
      override def MONTH_INDEX_OFFSET = 0

    }


    /** Поддержка Связывания Joda-time с контейнером MYmd. */
    implicit object JodaLocalDateYmdHelper extends IYmdHelper[JodaLocalDate] {

      override def now: JodaLocalDate = JodaLocalDate.now()

      override def plusDays(date: JodaLocalDate, days: Int): JodaLocalDate = {
        date.plusDays(days)
      }

      override def plusMonths(date: JodaLocalDate, months: Int): JodaLocalDate = {
        date.plusMonths(months)
      }

      override def yearDmonthDay2date(year: Int, dateMonth: Int, day: Int): JodaLocalDate = {
        new JodaLocalDate(year, dateMonth, day)
      }

      override def getDateMonthOfYear(date: JodaLocalDate): Int = {
        date.getMonthOfYear
      }

      override def getYear(date: JodaLocalDate): Int = {
        date.getYear
      }

      override def getDayOfMonth(date: JodaLocalDate): Int = {
        date.getDayOfMonth
      }

      /** Joda-time использует человеческую нумерацию месяцев. Сдвига нет. */
      override def MONTH_INDEX_OFFSET = 0

    }


    /** Недо-хелпер для интеграции MYmd с joda-time датами в режиме даты-времени, где время может быть любым. */
    implicit object JodaDateTimeYmdHelper extends IYmdHelper[JodaDateTime] {

      private def _localDateHelper = implicitly[IYmdHelper[JodaLocalDate]]

      override def now: JodaDateTime = JodaDateTime.now()

      override def plusDays(date: JodaDateTime, days: Int): JodaDateTime = {
        date.plusDays(days)
      }

      override def plusMonths(date: JodaDateTime, months: Int): JodaDateTime = {
        date.plusMonths(months)
      }

      override def yearDmonthDay2date(year: Int, dateMonth: Int, day: Int): JodaDateTime = {
        _localDateHelper
          .yearDmonthDay2date(year = year, dateMonth = dateMonth, day = day)
          .toDateTimeAtStartOfDay
      }

      override def getDateMonthOfYear(date: JodaDateTime): Int = {
        date.getMonthOfYear
      }

      override def getYear(date: JodaDateTime): Int = {
        date.getYear
      }

      override def getDayOfMonth(date: JodaDateTime): Int = {
        date.getDayOfMonth
      }

      override def MONTH_INDEX_OFFSET: Int = 0

    }

  }

}
