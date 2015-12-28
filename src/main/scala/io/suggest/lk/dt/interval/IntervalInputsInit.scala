package io.suggest.lk.dt.interval

import io.suggest.dt.interval.QuickAdvPeriodsT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 9:59
 * Description: Инициализации контролов ввода интервала дат.
 * Это абстрактная поддержка, т.е. нужно немного допилить её для корректной реакции на события.
 */
trait IntervalInputsInit {

  protected val periodsEnum: QuickAdvPeriodsT

  type PeriodEl_t = periodsEnum.T


  /** Запуск инициализации input'ов дат.  */
  def initDtIntervalInputs(): Unit = {

  }

}
