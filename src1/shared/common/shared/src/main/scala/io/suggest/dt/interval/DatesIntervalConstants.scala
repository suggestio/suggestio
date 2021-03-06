package io.suggest.dt.interval

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 14:07
 * Description: Константы для куска формы, который занимается выбором даты.
 */
object DatesIntervalConstants {

  /** Принятые в рамках sio номера дней недели. */
  def DAYS_OF_WEEK = 1 to 7

  /** Принятые в рамках sio номера месяцев года. */
  def MONTHS_OF_YEAR = 1 to 12


  /** Префикс id'шников элементов виджета выбора периода дат. */
  def ID_PREFIX           = "dsivl"

  /** id корневого контейнера виджета. */
  def CONT_ID             = ID_PREFIX + "C"


  /** id контейнера с опциями. */
  def OPTIONS_CONT_ID     = CONT_ID + "O"

  /** id селектора периода. */
  def PERIOD_SELECT_ID    = OPTIONS_CONT_ID + "P"


  /** id контейнера дат. */
  def DATES_CONT_ID       = OPTIONS_CONT_ID + "D"

  /** id инпута даты начала периода. */
  def DATE_START_INPUT_ID = DATES_CONT_ID + "S"

  /** id инпута даты окончания периода. */
  def DATE_END_INPUT_ID   = DATES_CONT_ID + "E"


  /** id контейнера с инфой по выбранному периоду. */
  def INFO_CONT_ID        = CONT_ID + "I"


  /** id input-элемента, хранящего в value json-строку с параметрами инициализации. */
  def INIT_ARGS_INPUT_ID  = "ias" + ID_PREFIX


  object Json {

    final val START_FN = "a"

    final val END_FN   = "b"

    final val DOW_FN   = "c"

    final val DATE_FN  = "d"

  }

}
