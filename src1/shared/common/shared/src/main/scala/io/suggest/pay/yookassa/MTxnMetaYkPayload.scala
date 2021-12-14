package io.suggest.pay.yookassa

import io.suggest.common.empty.EmptyProduct
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._


/** YooKassa MTxn.metadata.payload model. */
object MTxnMetaYkPayload {

  @inline implicit def univEq: UnivEq[MTxnMetaYkPayload] = UnivEq.derive

  implicit def txnMetaYkPayloadJson: OFormat[MTxnMetaYkPayload] = {
    (__ \ "deal")
      .formatNullable[MYkDeal]
      .inmap[MTxnMetaYkPayload]( apply, _.deal )
  }

}


/** Txn payload container in transaction.
  *
  * @param deal YooKassa deal information, if deal started around current transaction order.
  */
case class MTxnMetaYkPayload(
                              deal: Option[MYkDeal] = None,
                            )
  extends EmptyProduct
