package io.suggest.dt

import java.time.LocalDate

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
    implicit object LocalDateYmdHelper extends IYmdHelper[LocalDate] {

      override def now: LocalDate = LocalDate.now()

      override def plusDays(date: LocalDate, days: Int): LocalDate = {
        date.plusDays(days)
      }

      override def plusMonths(date: LocalDate, months: Int): LocalDate = {
        date.plusMonths(months)
      }

      override def yearDmonthDay2date(year: Int, dateMonth: Int, day: Int): LocalDate = {
        LocalDate.of(year, dateMonth, day)
      }

      override def getDateMonthOfYear(date: LocalDate): Int = {
        date.getMonthValue
      }

      override def getYear(date: LocalDate): Int = {
        date.getYear
      }

      override def getDayOfMonth(date: LocalDate): Int = {
        date.getDayOfMonth
      }

      /** Java-8 time использует человеческую нумерацию месяцев. Инопланетного сдвига нет. */
      override def MONTH_INDEX_OFFSET = 0

    }

  }

}
