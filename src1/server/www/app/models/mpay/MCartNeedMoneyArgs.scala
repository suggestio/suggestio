package models.mpay

import io.suggest.bill.cart.{MCartResolution, MCartSubmitQs}
import io.suggest.mbill2.m.contract.MContract
import io.suggest.mbill2.m.order.MOrderWithItems
import io.suggest.n2.node.MNode

/** Arguments container class for calling paySystem-related middleware util,
  * when cart order needs some additional money to be paid via concrete paysystem. */
case class MCartNeedMoneyArgs(
                               cartResolution   : MCartResolution,
                               cartOrder        : MOrderWithItems,
                               personContract   : MContract,
                               personNode       : MNode,
                               cartQs           : MCartSubmitQs,
                             )
