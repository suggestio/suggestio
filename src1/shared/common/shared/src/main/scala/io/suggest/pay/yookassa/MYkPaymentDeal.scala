package io.suggest.pay.yookassa

import io.suggest.common.empty.EmptyUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** @see [[https://yookassa.ru/developers/api#create_payment_deal_settlements]] */
case class MYkPaymentDeal(
                           id: String,
                           settlements: Seq[MYkPaymentDealSettlement],
                         )

object MYkPaymentDeal {

  @inline implicit def univEq: UnivEq[MYkPaymentDeal] = UnivEq.derive

  implicit def ykPaymentDealJson: OFormat[MYkPaymentDeal] = {
    (
      (__ \ "id").format[String] and
      (__ \ "settlements").formatNullable[Seq[MYkPaymentDealSettlement]]
        .inmap[Seq[MYkPaymentDealSettlement]](
          EmptyUtil.opt2ImplEmpty1F(Nil),
          sets => Option.when(sets.nonEmpty)(sets),
        )
    )(apply, unlift(unapply))
  }

}
