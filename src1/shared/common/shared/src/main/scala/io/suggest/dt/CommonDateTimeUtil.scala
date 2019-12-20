package io.suggest.dt

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, OffsetDateTime, ZoneOffset}

import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.mbill2.m.item.MItem
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


  /** Версия формата для переносимого json-array-формата даты.
    * Используются отрицательные числа, т.к. 0 и положительные числа используются для даты или её частей. */
  val OFFSET_DATE_TIME_FORMAT_VSN_1 = -1

  object Implicits {

    // Используем представление в виде JsArray[Int] от большего к меньшему, и tzHH,MM в самом хвосте.

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

        // Внутренний нестандартизированный человеко-читабельный sio-формат даты-времени: [VERSON, Y, M, D, H, i, S, ns, +sec]
        // Не совместим ни с чем, поэтому из-за elasticsearch его придётся дропнуть в будущем.
        case JsArray( valuesJsv ) =>
          try {
            // Явно кастовать collection.Seq вместо s.c.i.Seq, т.к. valuesJsv содержит collection.IndexedSeq = ArraySeq, но это может меняться от версии к версии.
            val collection.Seq(OFFSET_DATE_TIME_FORMAT_VSN_1, year, month, dayOfMonth, hour, minute, seconds, nanoSec, zoneOffsetTotalSeconds) =
              valuesJsv
                .iterator
                .map(_.asInstanceOf[JsNumber].value.toInt)
                .toSeq

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
        val fmt = o.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME )
        JsString(fmt)
        // Формат передачи даты, когда были сомнения в JSR-310 на scala.js
        /*Json.arr(
          OFFSET_DATE_TIME_FORMAT_VSN_1,
          o.getYear,
          o.getMonthValue,
          o.getDayOfMonth,
          o.getHour,
          o.getMinute,
          o.getSecond,
          o.getNano,
          o.getOffset.getTotalSeconds
        )*/
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

}
