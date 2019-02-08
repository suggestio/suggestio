package io.suggest.mbill2.m.txn

import io.suggest.bill.MPrice
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.09.18 18:55
  * Description: Связка MTxn + MPrice, т.к. в транзакции нет валюты, только дельты баланса до-после.
  * Возникла из-за необходимости как-то рендерить ценник транзакции client-side.
  */
object MTxnPriced {

  /** Поддержка play-json. */
  implicit def mTxnPricedFormat: OFormat[MTxnPriced] = (
    (__ \ "t").format[MTxn] and
    (__ \ "p").format[MPrice]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MTxnPriced] = UnivEq.derive

}

case class MTxnPriced(
                       txn    : MTxn,
                       price  : MPrice
                     )
