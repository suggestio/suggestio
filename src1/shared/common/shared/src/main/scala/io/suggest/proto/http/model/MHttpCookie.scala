package io.suggest.proto.http.model

import java.time.Instant

import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.dt.CommonDateTimeUtil.Implicits._

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
    final def RECEIVED_AT = "at"
  }

  implicit def httpCookieJson: Format[MHttpCookie] = {
    val F = Fields
    (
      (__ \ F.SET_COOKIE_HEADER_VALUE).format[String] and
      (__ \ F.RECEIVED_AT).format[Instant]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MHttpCookie] = UnivEq.derive

}


/** Контейнер данных по кукису, присланному сервером.
  *
  * @param setCookieHeaderValue Значение Set-Cookie заголовка, принятого от сервера.
  * @param receivedAt Данные даты-времени получения кукиса с сервера.
  */
final case class MHttpCookie(
                              setCookieHeaderValue    : String,
                              receivedAt              : Instant       = Instant.now(),
                            ) {

  /** Значение для Cookie: заголовка, отправляемое на сервер. */
  def cookieHeaderValue: String = {
    val untilIndex = setCookieHeaderValue.indexOf(';')
    setCookieHeaderValue.substring( 0, untilIndex )
  }

}
