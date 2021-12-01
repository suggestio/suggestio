package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/** @see [[https://yookassa.ru/developers/api#create_deal]] */
case class MYkDealCreate(
                          dealType        : MYkDealType         = MYkDealTypes.SafeDeal,
                          feeMoment       : MYkDealFeeMoment,
                          description     : Option[String]      = None,
                          metadata        : Option[JsObject]    = None,
                        )

object MYkDealCreate {

  @inline implicit def univEq: UnivEq[MYkDealCreate] = UnivEq.derive

  implicit def ykDealCreateJson: OFormat[MYkDealCreate] = {
    (
      (__ \ "type").format[MYkDealType] and
      (__ \ "fee_moment").format[MYkDealFeeMoment] and
      (__ \ "description").formatNullable[String] and
      (__ \ "metadata").formatNullable[JsObject]
    )(apply, unlift(unapply))
  }

}
