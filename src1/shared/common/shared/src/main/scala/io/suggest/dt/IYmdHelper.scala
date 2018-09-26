package io.suggest.dt

import java.time.LocalDate

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 19:31
  * Description: Интерфейс typeclass'ов для датовой арифметики без конкретного знания информации о типах.
  * На js одни даты, на сервере - другие. Тут общий интерфейс.
  *
  * Трейт различает понятия month и dateMonth:
  * Исторически, браузеры разрабатывались дикими человекообразными обезьянами с неизвестной планеты,
  * которые декабрь обозначали как 11, а январь - 0.
  * Трейт отрабатывает ситуацию связи с внеземными месяцеисчислениями с помощью MONTH_INDEX_OFFSET.
  * dateMonth -- номер месяца в понятиях Date_t. 1..12 или 0..11 или что-то ещё.
  * month -- человеческий номер месяца: 1..12.
  *
  * @tparam Date_t Тип инстансов платформенной даты.
  */
trait IYmdHelper[Date_t] {

  def now: Date_t

  def plusDays(date: Date_t, days: Int): Date_t

  def plusWeeks(date: Date_t, weeks: Int): Date_t = plusDays(date, weeks * 7)

  def plusMonths(date: Date_t, months: Int): Date_t

  /** Конверсия [[MYmd]] в Date_t. */
  def toDate(ymd: MYmd): Date_t = {
    yearDmonthDay2date(
      year      = ymd.year,
      dateMonth = ymdMonthToDateMonth(ymd.month),
      day       = ymd.day
    )
  }

  /** Конверсия из Date_t в [[MYmd]]. */
  def toYmd(date: Date_t): MYmd = {
    MYmd(
      year  = getYear(date),
      month = getYmdMonthOfYear(date),
      day   = getDayOfMonth(date)
    )
  }

  def yearDmonthDay2date(year: Int, dateMonth: Int, day: Int): Date_t

  def getDateMonthOfYear(date: Date_t): Int
  def getYmdMonthOfYear(date: Date_t): Int = {
    dateMonthToYmdMonth( getDateMonthOfYear(date) )
  }

  def getYear(date: Date_t): Int

  def getDayOfMonth(date: Date_t): Int

  /** Сдвиг нумерации месяцев.
    * @return 0 значит месяцы нумеруются по-человечески 1-12 и по ISO.
    *         -1 значит, что нумерация будет по-браузерному: т.е. 0-11.
    */
  def MONTH_INDEX_OFFSET: Int

  /**
    * Приведение человеческой нумерации месяцев 1-12 к нумерации, используемой в Date_t.
    * Например, js.Date используется 0-11.
    */
  def ymdMonthToDateMonth(month_1_12: Int): Int = {
    month_1_12 + MONTH_INDEX_OFFSET
  }

  /** Приведение платформенного месяца в понятиях Date_t к нормальному человеческому 1-12. */
  def dateMonthToYmdMonth(dateMonth: Int): Int = {
    dateMonth - MONTH_INDEX_OFFSET
  }

}


trait Month0Indexed[Date_t] extends IYmdHelper[Date_t] {
  override def MONTH_INDEX_OFFSET = -1
}


object IYmdHelper {

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
