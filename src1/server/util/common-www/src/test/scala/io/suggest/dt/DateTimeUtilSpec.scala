package io.suggest.dt

import java.time.{ZoneId, ZonedDateTime}

import org.scalatest.Matchers._
import org.scalatest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.02.17 12:14
  * Description: Тесты для серверной утили даты-времени: [[DateTimeUtil]].
  */
class DateTimeUtilSpec extends FlatSpec {

  "minutesOffset2TzOff()" should "handle MSK tz from browser" in {
    // Время на стороне сервера:
    val now = ZonedDateTime.of(2017, 2, 6, 1, 1, 1, 0, ZoneId.of("Europe/Moscow"))
    // Оффсет времени на стороне сервера:
    val nowTzOff = now.getOffset

    // Присланный браузером сдвиг в минутах.
    val browserOffsetMinutes = -180
    // Оффсет времени на стороне клиента по мнению сервера:
    val browserTzOff = CommonDateTimeUtil.minutesOffset2TzOff(browserOffsetMinutes)

    nowTzOff shouldEqual browserTzOff
    nowTzOff.getTotalSeconds shouldEqual browserTzOff.getTotalSeconds
  }

}
