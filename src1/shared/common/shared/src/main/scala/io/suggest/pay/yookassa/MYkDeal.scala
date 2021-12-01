package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/** @see [[https://yookassa.ru/developers/api#deal_object_safe_deal_type]] */
case class MYkDeal(
                    id              : String,
                    dealType        : MYkDealType         = MYkDealTypes.SafeDeal,
                    feeMoment       : MYkDealFeeMoment,
                    description     : Option[String]      = None,
                    balance         : MYkAmount,
                    payoutBalance   : MYkAmount,
                    status          : MYkDealStatus,
                    createdAt       : String, // TODO ISO 8601 date-time UTC
                    expiresAt       : String, // TODO ISO 8601 date-time UTC
                    metadata        : Option[JsObject]    = None,
                    test            : Boolean,
                  )

object MYkDeal {

  @inline implicit def univEq: UnivEq[MYkDeal] = UnivEq.derive

  implicit def ykDealFormat: OFormat[MYkDeal] = {
    (
      (__ \ "id").format[String] and
      (__ \ "type").format[MYkDealType] and
      (__ \ "fee_moment").format[MYkDealFeeMoment] and
      (__ \ "description").formatNullable[String] and
      (__ \ "balance").format[MYkAmount] and
      (__ \ "payout_balance").format[MYkAmount] and
      (__ \ "status").format[MYkDealStatus] and
      (__ \ "created_at").format[String] and
      (__ \ "expires_at").format[String] and
      (__ \ "metadata").formatNullable[JsObject] and
      (__ \ "test").format[Boolean]
    )(apply, unlift(unapply))
  }

}


