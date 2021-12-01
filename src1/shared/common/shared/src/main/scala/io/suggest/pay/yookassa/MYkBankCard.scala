package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** @see [[https://yookassa.ru/developers/api#create_payout_payout_destination_data_bank_card_card]] */
case class MYkBankCard(
                        number: String,
                      )

object MYkBankCard {

  @inline implicit def univEq: UnivEq[MYkBankCard] = UnivEq.derive

  implicit def ykBankCardJson: OFormat[MYkBankCard] = {
    (__ \ "number")
      .format[String]
      .inmap[MYkBankCard]( apply, _.number )
  }

}
