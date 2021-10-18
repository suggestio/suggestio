package io.suggest.bill.cart

import io.suggest.bill.MPrice
import io.suggest.mbill2.m.balance.MBalance
import io.suggest.mbill2.m.order.MOrderWithItems
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** Container for cart resolution data after cart processing (after pay request).
  *
  * @param idea What is done or need to be done.
  * @param newCart Updated order, if updated.
  * @param newBalances Updated user balances, if any.
  * @param needMoney Requesting for more money, if needed.
  */
case class MCartResolution(
                            idea          : MCartIdea,
                            newCart       : Option[MOrderWithItems]   = None,
                            newBalances   : Option[Seq[MBalance]]     = None,
                            needMoney     : Option[Seq[MPrice]]       = None,
                          )

object MCartResolution {

  @inline implicit def univEq: UnivEq[MCartResolution] = UnivEq.derive

  implicit def cartResolutionJson: OFormat[MCartResolution] = {
    (
      (__ \ "idea").format[MCartIdea] and
      (__ \ "new_cart").formatNullable[MOrderWithItems] and
      (__ \ "new_balances").formatNullable[Seq[MBalance]] and
      (__ \ "need_money").formatNullable[Seq[MPrice]]
    )(apply, unlift(unapply))
  }

}
