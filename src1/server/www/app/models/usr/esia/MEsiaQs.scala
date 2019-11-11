package models.usr.esia

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.util.logs.MacroLogsImpl
import play.api.mvc.QueryStringBindable

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.03.19 14:37
  * Description: Утиль для qs-моделей ЕСИА.
  */
object MEsiaQs extends MacroLogsImpl {

  def TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern( "yyyy.MM.dd HH:mm:ss Z" )

  /** Поддержка offset date time <=> qs маппинга. */
  implicit def esiaTimestampQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[OffsetDateTime] = {
    new QueryStringBindableImpl[OffsetDateTime] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, OffsetDateTime]] = {
        for {
          timestampStrE <- strB.bind(key, params)
        } yield {
          for {
            timestampStr <- timestampStrE

            temporal <- Try(
              TIMESTAMP_FORMAT.parse(timestampStr)
            )
              .toEither
              .left.map { ex =>
                val msg = s"Cannot parse: $timestampStr"
                LOGGER.warn(msg, ex)
                msg
              }

            offDt <- Try(
              OffsetDateTime.from( temporal )
            )
              .toEither
              .left.map { ex =>
                val msg = s"Cannot convert '${temporal}' to OffsetDateTime"
                LOGGER.warn(msg, ex)
                msg
              }

          } yield {
            offDt
          }
        }
      }

      override def unbind(key: String, value: OffsetDateTime): String =
        strB.unbind(key, TIMESTAMP_FORMAT.format( value ))

    }
  }

}
