package io.suggest.dt

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset, LocalDate => Java8LocalDate}

import com.google.inject.Singleton

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


    /** Недо-хелпер для интеграции MYmd с local-time вместо голых дат, где время может быть любым. */
    @deprecated("Do not use this", "2016.jan.26")
    object LocalDateTimeYmdHelper extends IYmdHelper[LocalDateTime] {

      private def _localDateHelper = implicitly[IYmdHelper[Java8LocalDate]]

      override def now: LocalDateTime = LocalDateTime.now()

      override def plusDays(date: LocalDateTime, days: Int): LocalDateTime = {
        date.plusDays(days)
      }

      override def plusMonths(date: LocalDateTime, months: Int): LocalDateTime = {
        date.plusMonths(months)
      }

      override def yearDmonthDay2date(year: Int, dateMonth: Int, day: Int): LocalDateTime = {
        _localDateHelper
          .yearDmonthDay2date(year = year, dateMonth = dateMonth, day = day)
          .atStartOfDay()
      }

      override def getDateMonthOfYear(date: LocalDateTime): Int = {
        date.getMonthValue
      }

      override def getYear(date: LocalDateTime): Int = {
        date.getYear
      }

      override def getDayOfMonth(date: LocalDateTime): Int = {
        date.getDayOfMonth
      }

      override def MONTH_INDEX_OFFSET = _localDateHelper.MONTH_INDEX_OFFSET

    }

    @deprecated("Do not use this", "2016.jan.26")
    implicit object OffsetDateTimeYmdHelper extends IYmdHelper[OffsetDateTime] {

      private def _localDateHelper = LocalDateTimeYmdHelper //implicitly[IYmdHelper[LocalDateTime]]

      override def now: OffsetDateTime = OffsetDateTime.now()

      override def plusDays(date: OffsetDateTime, days: Int): OffsetDateTime = {
        date.plusDays(days)
      }

      override def plusMonths(date: OffsetDateTime, months: Int): OffsetDateTime = {
        date.plusMonths(months)
      }

      override def yearDmonthDay2date(year: Int, dateMonth: Int, day: Int): OffsetDateTime = {
        _localDateHelper
          .yearDmonthDay2date(year = year, dateMonth = dateMonth, day = day)
          .atOffset( ZoneOffset.UTC )
      }

      override def getDateMonthOfYear(date: OffsetDateTime): Int = {
        date.getMonthValue
      }

      override def getYear(date: OffsetDateTime): Int = {
        date.getYear
      }

      override def getDayOfMonth(date: OffsetDateTime): Int = {
        date.getDayOfMonth
      }

      override def MONTH_INDEX_OFFSET = _localDateHelper.MONTH_INDEX_OFFSET

    }

  }

}
