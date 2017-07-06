package io.suggest.dt

import scalaz.{Validation, ValidationNel}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.17 18:31
  * Description: Очень общая клиент-серверная утиль для работы с датами, временем, часовыми поясами и зонами.
  */
object CommonDateTimeUtil {

  /** Валидация данных по текущему часовому поясу, которые сообщает браузер из js.Date.
    *
    * @param tzOffMinutes Сдвиг в минутах относильно UTC.
    */
  def jsDateTzOffsetMinutesV(tzOffMinutes: Int): ValidationNel[String, Int] = {
    Validation.liftNel(tzOffMinutes)( Math.abs(_) > 660, "e.tz.offset.minutes" )
  }

}
