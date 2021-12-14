package io.suggest.mbill2.m.txn

import io.suggest.bill.MPrice
import io.suggest.bill.cart.MCartSubmitQs
import io.suggest.common.empty.{EmptyProduct, EmptyUtil}
import io.suggest.mbill2.m.gid.Gid_t
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._


/** Metadata JSON container for [[MTxn]].
  *
  * @param cartQs Cart submit URL query string.
  * @param moneyRcvrs Money final receivers information.
  * @param psPayload Pay-system implementation-depend information.
  */
case class MTxnMetaData(
                         cartQs         : Option[MCartSubmitQs]       = None,
                         moneyRcvrs     : Seq[MMoneyReceiverInfo]     = Nil,
                         psPayload      : Option[JsValue]             = None,
                       )
  extends EmptyProduct


object MTxnMetaData {

  @inline implicit def univEq: UnivEq[MTxnMetaData] = UnivEq.derive

  implicit def txnMetaDataJson: OFormat[MTxnMetaData] = {
    (
      (__ \ "cartQs").formatNullable[MCartSubmitQs] and
      (__ \ "moneyReceivers").formatNullable[Seq[MMoneyReceiverInfo]]
        .inmap [Seq[MMoneyReceiverInfo]] (
          EmptyUtil.opt2ImplEmptyF( Nil ),
          seq => Option.when(seq.nonEmpty)(seq),
        ) and
      (__ \ "psPayload").formatNullable[JsValue]
    )(apply, unlift(unapply))
  }

}


/** Minimal information about some money receiver of transaction. */
case class MMoneyReceiverInfo(
                              contractId      : Gid_t,
                              price           : MPrice,
                              isSioComission  : Boolean,
                            )
object MMoneyReceiverInfo {

  @inline implicit def univEq: UnivEq[MMoneyReceiverInfo] = UnivEq.derive

  implicit def moneyReceiverInfoJson: OFormat[MMoneyReceiverInfo] = {
    (
      (__ \ "contractId").format[Gid_t] and
      (__ \ "price").format[MPrice] and
      (__ \ "isSioComission").format[Boolean]
    )(apply, unlift(unapply))
  }

}