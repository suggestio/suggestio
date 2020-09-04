package io.suggest.dt

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, OffsetDateTime, ZoneOffset}

import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.mbill2.m.item.MItem
import play.api.libs.json._
import scalaz.{Validation, ValidationNel}

import scala.util.Try

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


  /**
    * Из JS могут приходить данные о тайм-зоне браузера в виде кол-ва минут относительно UTC.
    * Этот метод приводит это кол-во минут к ZoneOffset.
    *
    * @param minutes Кол-во минут сдвига относительно UTC.
    * @return ZoneOffset.
    */
  def minutesOffset2TzOff(minutes: Int): ZoneOffset = {
    ZoneOffset.ofTotalSeconds( -minutes * 60 )
  }


  object Implicits {

    /** Поддержка JSON-сериализации для Instant. */
    implicit def instantJson: Format[Instant] = {
      Format[Instant](
        Reads {
          case JsNumber(value) =>
            Try(
              Instant.ofEpochMilli( value.toLong )
            )
              .fold(
                {ex =>
                  JsError( ex.getMessage )
                },
                JsSuccess(_),
              )
          case other =>
            JsError( other.toString() )
        },
        Writes { inst =>
          JsNumber( inst.toEpochMilli )
        }
      )
    }

    /** Поддержка десериализации java.time.OffsetDateTime из sio-формата в виде json-массива. */
    implicit def offsetDateTimeReads: Reads[OffsetDateTime] = {
      Reads[OffsetDateTime] {
        // Для кросс-платформенных моделей:
        case JsString(formatted) =>
          try {
            JsSuccess( OffsetDateTime.parse( formatted, DateTimeFormatter.ISO_OFFSET_DATE_TIME ) )
          } catch { case ex: Throwable =>
            JsError("dtFmt? " + ex)
          }
        case other =>
          JsError( "[]? " + other )
      }
    }

    /** Поддержка сериализации даты-времени OffsetDateTime в Array[Int]-формат. */
    implicit def offsetDateTimeWrites: Writes[OffsetDateTime] = {
      Writes[OffsetDateTime] { o =>
        val fmt = o.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME )
        JsString(fmt)
      }
    }


    implicit def offsetDateTimeFormat: Format[OffsetDateTime] =
      Format( offsetDateTimeReads, offsetDateTimeWrites )


    /** Доп.костыли для LocalDate. */
    implicit class LocalDateOpsExt( val ld: LocalDate ) extends AnyVal {

      /** Конвертация в легаси MYmd. */
      def toYmd: MYmd = {
        MYmd(
          year  = ld.getYear,
          month = ld.getMonthValue,
          day   = ld.getDayOfMonth
        )
      }

    }


    implicit class ItemDtOpsExt( val mitem: MItem ) extends AnyVal {

      def dtToRangeYmdOpt: MRangeYmdOpt = {
        def _offDt2YmdF(offDt: OffsetDateTime) = offDt.toLocalDate.toYmd
        MRangeYmdOpt(
          dateStartOpt = mitem.dateStartOpt.map(_offDt2YmdF),
          dateEndOpt   = mitem.dateEndOpt.map(_offDt2YmdF)
        )
      }

    }

  }


  object Regexp {

    // One of "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", or "Sun" (case-sensitive).
    def dow3S = "(Mon|Tue|Wed|Thu|Fri|Sat|Sun)"

    // 2 digit day number, e.g. "04" or "23".
    def dayOfMonthS = "[0-9]{1,2}"

    def monthNameS = "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)"

    def year4S = "[0-9]{4}"

    def hour24S = "[012][0-9]"
    def minuteS = "[0-5][0-9]"
    def secondS = minuteS

    def gmt = "GMT"

    def rfc1123Dt = s"$dow3S,\\s+$dayOfMonthS\\s+$monthNameS\\s+$year4S\\s+$hour24S:$minuteS:$secondS\\s+GMT".r

  }

}
