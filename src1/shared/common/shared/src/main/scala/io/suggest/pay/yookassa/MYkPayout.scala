package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/** @see [[https://yookassa.ru/developers/api#payout_object]] */
case class MYkPayout(
                      id                  : String,
                      amount              : MYkAmount,
                      status              : MYkPayoutStatus,
                      payoutDestination   : MYkPayoutDestination,
                      description         : Option[String]        = None,
                      createdAt           : String, // TODO ISO 8601 UTC datetime.
                      deal                : Option[MYkPayoutDeal] = None,
                      cancellationDetails : Option[MYkCancellationDetails] = None,
                      metadata            : Option[JsObject]      = None,
                      test                : Boolean,
                    )

object MYkPayout {

  @inline implicit def univEq: UnivEq[MYkPayout] = UnivEq.derive

  implicit def ykPayoutJson: OFormat[MYkPayout] = {
    (
      (__ \ "id").format[String] and
      (__ \ "amount").format[MYkAmount] and
      (__ \ "status").format[MYkPayoutStatus] and
      (__ \ "payout_destination").format[MYkPayoutDestination] and
      (__ \ "description").formatNullable[String] and
      (__ \ "created_at").format[String] and
      (__ \ "deal").formatNullable[MYkPayoutDeal] and
      (__ \ "cancellation_details").formatNullable[MYkCancellationDetails] and
      (__ \ "metadata").formatNullable[JsObject] and
      (__ \ "test").format[Boolean]
    )(apply, unlift(unapply))
  }

}
