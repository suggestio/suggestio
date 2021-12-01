package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** @see [[https://yookassa.ru/developers/api#create_payout_payout_destination_data]] */
case class MYkPayoutDestinationData(
                                     typ            : MYkPayoutDestinationType,
                                     card           : Option[MYkBankCard] = None,
                                     accountNumber  : Option[String] = None,
                                   )

object MYkPayoutDestinationData {

  @inline implicit def univEq: UnivEq[MYkPayoutDestinationData] = UnivEq.derive

  implicit def ykPayoutDestinationDataJson: OFormat[MYkPayoutDestinationData] = {
    (
      (__ \ "type").format[MYkPayoutDestinationType] and
      (__ \ "card").formatNullable[MYkBankCard] and
      (__ \ "account_number").formatNullable[String]
    )(apply, unlift(unapply))
  }

}
