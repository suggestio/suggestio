package io.suggest.dt

import java.time.{LocalDate, OffsetDateTime, ZoneOffset}

import play.api.libs.json._
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


  /** Версия формата для переносимого json-array-формата даты.
    * Используются отрицательные числа, т.к. 0 и положительные числа используются для даты или её частей. */
  val OFFSET_DATE_TIME_FORMAT_VSN_1 = -1

  object Implicits {

    // Используем представление в виде JsArray[Int] от большего к меньшему, и tzHH,MM в самом хвосте.

    /** Поддержка десериализации java.time.OffsetDateTime из sio-формата в виде json-массива. */
    implicit def offsetDateTimeReads: Reads[OffsetDateTime] = {
      Reads[OffsetDateTime] {
        // Внутренний нестандартизированный человеко-читабельный sio-формат даты-времени: [VERSON, Y, M, D, H, i, S, ns, +sec]
        // Не совместим ни с чем, поэтому из-за elasticsearch его придётся дропнуть в будущем.
        case JsArray( valuesJsv ) =>
          try {
            val Seq(OFFSET_DATE_TIME_FORMAT_VSN_1, year, month, dayOfMonth, hour, minute, seconds, nanoSec, zoneOffsetTotalSeconds) =
              valuesJsv
                .asInstanceOf[Seq[JsNumber]]
                // O(n)-операция, не делающая ровным счётом ничего. Для ускорения можно попробовать .asInstanceOf[Int].
                .map(_.value.toInt)

            val zoneOffset = ZoneOffset.ofTotalSeconds( zoneOffsetTotalSeconds )
            val offsetDt = OffsetDateTime.of( year, month, dayOfMonth, hour, minute, seconds, nanoSec, zoneOffset )
            JsSuccess( offsetDt )
          } catch {
            case ex: Throwable =>
              JsError("dt? " + ex)
          }

        // TODO Для совместимости с ES и для простоты надо заюзать epoch-millis, но scala.js имеет проблемы с Long. Использовать Int без миллисекунд?
        /*
        case JsNumber(epochSecondsUtcBd) =>
          val zoneOffset = ZoneOffset.UTC
          try {
            val offsetDt = LocalDateTime
              .ofEpochSecond( epochSecondsUtcBd.toLong, 0, zoneOffset )
              .atOffset( zoneOffset )
            JsSuccess( offsetDt )
          } catch {
            case ex: Throwable =>
              JsError("epochS? " + ex)
          }
        */
        case other =>
          JsError( "[]? " + other )
      }
    }

    /** Поддержка сериализации даты-времени OffsetDateTime в Array[Int]-формат. */
    implicit def offsetDateTimeWrites: Writes[OffsetDateTime] = {
      Writes[OffsetDateTime] { o =>
        Json.arr(
          OFFSET_DATE_TIME_FORMAT_VSN_1,
          o.getYear,
          o.getMonthValue,
          o.getDayOfMonth,
          o.getHour,
          o.getMinute,
          o.getSecond,
          o.getNano,
          o.getOffset.getTotalSeconds
        )
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

  }

}
