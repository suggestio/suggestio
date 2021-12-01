package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** @see [[https://yookassa.ru/developers/api#payout_object_deal]] */
case class MYkPayoutDeal(
                          id: String,
                        )

object MYkPayoutDeal {

  @inline implicit def univEq: UnivEq[MYkPayoutDeal] = UnivEq.derive

  implicit def ykPayoutDealJson: OFormat[MYkPayoutDeal] = {
    (__ \ "id")
      .format[String]
      .inmap[MYkPayoutDeal](apply, _.id)
  }

}

