package io.suggest.bill.cart.m

import io.suggest.bill.cart.MOrderContent
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.spa.DAction

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.18 11:13
  * Description: Cart form actions.
  */

sealed trait ICartAction extends DAction


/** Item's selection checkbox change state.
  * @param itemId item id .
  *               None - all items.
  * @param checked New checkbox state.
  */
case class CartSelectItem(itemId: Option[Gid_t], checked: Boolean) extends ICartAction

/** Delete selected items action. */
case object CartDeleteBtnClick extends ICartAction


/** Get current order contents from server.
  * @param orderId order id.
  *                None - Cart order.
  */
case class GetOrderContent(orderId: Option[Gid_t]) extends ICartAction

/** Load current order. Current, means, according to Cart Form configuration. */
case object LoadCurrentOrder extends ICartAction

/** GetOrderContent resulting action. */
case class HandleOrderContentResp(
                                   tryResp      : Try[MOrderContent],
                                   timestampMs  : Long
                                 )
  extends ICartAction
