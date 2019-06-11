package io.suggest.id.token

import java.time.Instant

import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.06.19 18:56
  * Description: Модель данных одного проверочного сообщения.
  */
object MIdMessageData {

  implicit def mMessageCheckDataJson: OFormat[MIdMessageData] = (
    (__ \ "a").format[Instant] and
    (__ \ "i").formatNullable[String] and
    (__ \ "c").format[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MIdMessageData] = UnivEq.derive

}

/** Контейнер метаданных одного отправленного сообщения идентификации.
  *
  * @param sentAt Когда отправлено.
  * @param msgId id отправленного сообщения, если есть.
  * @param checkCode Секретный код проверки.
  */
case class MIdMessageData(
                           sentAt      : Instant,
                           msgId       : Option[String],
                           checkCode   : String,
                         )
