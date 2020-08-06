package io.suggest.proto.http.model

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.08.2020 9:43
  * Description: Модель данных по csrf-токену, который с сервера отправляется на клиент в виде JSON.
  *
  * Приходит с сервера, и передаётся в HttpClient через конфиг, если js-роутер не может обеспечить CSRF сам.
  * Например, для login или иных форм, интегрированных внутрь выдачи, которая запихнута в мобильное приложение.
  */
object MCsrfToken {

  object Fields {
    def QS_KEY = "k"
    def VALUE = "v"
  }

  implicit def csrfTokenJson: OFormat[MCsrfToken] = {
    val F = Fields
    (
      (__ \ F.QS_KEY).format[String] and
      (__ \ F.VALUE).format[String]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MCsrfToken] = UnivEq.derive

}


/** Контейнер данных CSRF-токена.
  *
  * @param qsKey Ключ в URL query string.
  * @param value Значение токена.
  */
case class MCsrfToken(
                       qsKey        : String,
                       value        : String,
                       // TODO validTill? Или токен не истекает до конца сессии? Надо разобраться.
                     )
