package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/** @see [[https://yookassa.ru/developers/api#create_payout]] */
case class MYkPayoutCreate(
                            amount                  : MYkAmount,
                            payoutDestinationData   : Option[MYkPayoutDestinationData] = None,
                            payoutToken             : Option[String]                = None,
                            description             : Option[String]                = None,
                            deal                    : Option[MYkPayoutDeal]         = None,
                            metadata                : Option[JsObject]              = None,
                          )

object MYkPayoutCreate {

  @inline implicit def univEq: UnivEq[MYkPayoutCreate] = UnivEq.derive

  implicit def ykPayoutCreateJson: OFormat[MYkPayoutCreate] = {
    (
      (__ \ "amount").format[MYkAmount] and
      (__ \ "payout_destination_data").formatNullable[MYkPayoutDestinationData] and
      (__ \ "payout_token").formatNullable[String] and
      (__ \ "description").formatNullable[String] and
      (__ \ "deal").formatNullable[MYkPayoutDeal] and
      (__ \ "metadata").formatNullable[JsObject]
    )(apply, unlift(unapply))
  }

}
