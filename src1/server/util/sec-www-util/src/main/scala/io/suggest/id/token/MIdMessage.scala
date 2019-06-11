package io.suggest.id.token

import io.suggest.model.n2.edge.MPredicate
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.06.19 18:56
  * Description: Модель данных о проверочном сообщении.
  */
object MIdMessage {

  implicit def mIdMessageJson: OFormat[MIdMessage] = (
    (__ \ "t").format[MPredicate] and
    (__ \ "p").format[Set[String]] and
    (__ \ "m").format[Seq[MIdMessageData]]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MIdMessage] = UnivEq.derive

}


/** Описание хода проверки.
  *
  * @param rcpt Получатель. Для типа Phone здесь - номер телефона.
  * @param rcptType Тип получателя (Ident-предикат).
  *                 Обычно - MPredicates.Ident.Phone (номер телефона)
  * @param messages Список отправленных сообщений.
  */
case class MIdMessage(
                       rcptType     : MPredicate,
                       rcpt         : Set[String],
                       messages     : Seq[MIdMessageData],
                     )
