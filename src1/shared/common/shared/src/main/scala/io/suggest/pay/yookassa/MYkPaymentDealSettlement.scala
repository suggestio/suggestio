package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class MYkPaymentDealSettlement(
                                     typ      : MYkPaymentDealSettlementType = MYkPaymentDealSettlementTypes.Payout,
                                     amount   : MYkAmount,
                                   )

object MYkPaymentDealSettlement {

  @inline implicit def univEq: UnivEq[MYkPaymentDealSettlement] = UnivEq.derive

  implicit def ykPaymentDealSettlementJson: OFormat[MYkPaymentDealSettlement] = {
    (
      (__ \ "type").format[MYkPaymentDealSettlementType] and
      (__ \ "amount").format[MYkAmount]
    )(apply, unlift(unapply))
  }

}
