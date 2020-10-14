package io.suggest.proto.http.cookie

import java.time.Instant

import io.suggest.dt.CommonDateTimeUtil.Implicits._
import japgolly.univeq._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.2020 17:26
  * Description: Scala-модель HTTP-кукиса.
  *
  * Обычно кукисами занимается браузер, но в случае cordova управлять приходится руками.
  * Модель обязана быть сериализуемой для долгосрочного хранения.
  */
object MHttpCookie {

  object Fields {
    final def SET_COOKIE_HEADER_VALUE = "schv"
    private[MHttpCookie] final def RECEIVED_AT = "at"
    final def META = "m"
  }

  implicit def httpCookieJson: Format[MHttpCookie] = {
    val F = Fields
    (
      (__ \ F.SET_COOKIE_HEADER_VALUE).format[String] and {
        val fmtNormal = (__ \ F.META).format[MCookieMeta]
        // TODO 2020-10-14 Удалить readsFallbacked через неделю-полторы, после или к моменту релиза мобильного приложения v1.1 (или v2).
        val readsFallbacked = fmtNormal.orElse {
          (__ \ F.RECEIVED_AT).format[Instant]
            .map { at =>
              MCookieMeta(receivedAt = at)
            }
        }
        OFormat( readsFallbacked, fmtNormal )
      }
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MHttpCookie] = UnivEq.derive

}


/** Контейнер данных по кукису, присланному сервером.
  *
  * @param setCookieHeaderValue Значение Set-Cookie заголовка, принятого от сервера.
  * @param meta Какие-либо метаданные кукиса.
  */
final case class MHttpCookie(
                              setCookieHeaderValue    : String,
                              meta                    : MCookieMeta,
                            ) {

  /** Значение для Cookie: заголовка, отправляемое на сервер. */
  def cookieHeaderValue: String = {
    val untilIndex = setCookieHeaderValue.indexOf(';')
    setCookieHeaderValue.substring( 0, untilIndex )
  }

}
