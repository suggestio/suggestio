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

  def toYmd(date: Date_t): MYmd

  def getMonthOfYear(date: Date_t): Int

  def getYear(date: Date_t): Int

  def getDayOfMonth(date: Date_t): Int

  /**
    * Приведение человеческой нумерации месяцев 1-12 к нумерации, используемой в Date_t.
    * Например, js.Date используется 0-11.
    */
  def month12ToPlatformMonth(month: Int): Int

}
