package io.suggest.dt

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 19:31
  * Description: Интерфейс typeclass'ов для датовой арифметики без конкретного знания информации о типах.
  * На js одни даты, на сервере - другие. Тут общий интерфейс.
  */
trait IDtHelper[Date_t] {

  def now: Date_t

  def plusDays(date: Date_t, days: Int): Date_t

  def plusWeeks(date: Date_t, weeks: Int): Date_t = plusDays(date, weeks * 7)

  def plusMonths(date: Date_t, months: Int): Date_t

  def fromYmd(ymd: MYmd): Date_t

  def toYmd(date: Date_t): MYmd = {
    MYmd(
      year  = getYear(date),
      month = getYmdMonthOfYear(date),
      day   = getDayOfMonth(date)
    )
  }

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

trait Month0Indexed[Date_t] extends IDtHelper[Date_t] {
  override def MONTH_INDEX_OFFSET = -1
}
