package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** @see [[https://yookassa.ru/developers/api#payout_object_payout_destination]] */
case class MYkPayoutDestination(
                                 typ            : MYkPayoutDestinationType,
                                 card           : Option[MYkBankCardInfo] = None,
                                 accountNumber  : Option[String] = None,
                               )

object MYkPayoutDestination {

  @inline implicit def univEq: UnivEq[MYkPayoutDestination] = UnivEq.derive

  implicit def payoutDestinationJson: OFormat[MYkPayoutDestination] = {
    (
      (__ \ "type").format[MYkPayoutDestinationType] and
      (__ \ "card").formatNullable[MYkBankCardInfo] and
      (__ \ "account_number").formatNullable[String]
    )(apply, unlift(unapply))
  }

}
