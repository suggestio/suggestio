package io.suggest.bill.cart.m

import diode.data.Pot
import io.suggest.bill.cart.{MCartSubmitResult, MOrderContent}
import io.suggest.i18n.MMessage
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

/** Actions for OrderItemsAh. */
sealed trait IOrderItemsAction extends ICartAction

/** Item's selection checkbox change state.
  * @param itemId item id .
  *               None - all items.
  * @param checked New checkbox state.
  */
case class CartSelectItem(itemId: Option[Gid_t], checked: Boolean) extends IOrderItemsAction

/** Delete selected items action. */
case object CartDeleteBtnClick extends IOrderItemsAction


/** Get current order contents from server.
  * @param orderId order id.
  *                None - Cart order.
  */
case class GetOrderContent(orderId: Option[Gid_t]) extends IOrderItemsAction


case class UnHoldOrderDialogOpen( isOpen: Boolean ) extends IOrderItemsAction

/** Cancel order hold status steps.
  * No request timestamp here, because response describes altering server state.
  */
case class UnHoldOrderRequest(
                               timestampMs     : Long                        = System.currentTimeMillis(),
                               tryRespOpt      : Option[Try[MOrderContent]]  = None,
                             )
  extends IOrderItemsAction


sealed trait IBillConfAction extends ICartAction
/** Load current order. Current, means, according to Cart Form configuration. */
case object LoadCurrentOrder extends IBillConfAction

/** GetOrderContent resulting action. */
case class HandleOrderContentResp(
                                   tryResp      : Try[MOrderContent],
                                   timestampMs  : Long
                                 )
  extends IOrderItemsAction


/** Actions for CartPayAh. */
sealed trait ICartPayAction extends ICartAction

/** Start payment procedure (request to server). */
case class CartSubmit(
                       state        : Pot[MCartSubmitResult]          = Pot.empty,
                       timestampMs  : Long                            = System.currentTimeMillis(),
                     )
  extends ICartPayAction


/** PaySystem js-script initialization steps. */
case class PaySystemJsInit(
                            scriptLoadResult: Either[MMessage, None.type],
                          )
  extends ICartPayAction
