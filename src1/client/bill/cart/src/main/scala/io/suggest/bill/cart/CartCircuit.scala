package io.suggest.bill.cart

import diode.react.ReactConnector
import io.suggest.bill.cart.c.{BillConfAh, CartPayAh, ILkCartApi, LkCartApiHttpImpl, OrderItemsAh}
import io.suggest.bill.cart.m.{IBillConfAction, ICartPayAction, IOrderItemsAction, LoadCurrentOrder, MCartRootS, MOrderItemsS}
import io.suggest.bill.cart.u.CartUtil
import io.suggest.msg.ErrorMsgs
import io.suggest.log.CircuitLog
import io.suggest.spa.{CircuitUtil, StateInp}
import play.api.libs.json.Json
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 10:57
  * Description: Cart form circuit.
  */
object CartCircuit {

  /** Read & deserialize initial Form state from html input.
    * @return MCartRootS() instance.
    */
  def mkInitialModelFromInput(): MCartRootS = {
    val stateInp = StateInp.find().get
    val json = stateInp.value.get
    val minit = Json
      .parse(json)
      .as[MCartInit]

    MCartRootS(
      conf = minit.conf,
      order = MOrderItemsS(
        jdRuntime = CartUtil.mkJdRuntime()
      )
    )
  }

}


/** Mostly-implemented cart form circuit for different implementations. */
abstract class CartCircuitBase
  extends CircuitLog[MCartRootS]
  with ReactConnector[MCartRootS]
{

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.CART_CIRCUIT_ERROR


  // Models
  private def orderRW = CircuitUtil.mkLensRootZoomRW(this, MCartRootS.order)
  private def confRW = CircuitUtil.mkLensRootZoomRW(this, MCartRootS.conf)
  private def payRW = CircuitUtil.mkLensRootZoomRW( this, MCartRootS.pay )


  // APIs for controllers
  def lkCartApi: ILkCartApi = new LkCartApiHttpImpl


  // Controllers
  private def orderItemsAh: HandlerFunction = new OrderItemsAh(
    lkCartApi   = lkCartApi,
    modelRW     = orderRW
  )

  private def billConfAh: HandlerFunction = new BillConfAh(
    modelRW = confRW
  )

  private def cartPayAh: HandlerFunction = new CartPayAh(
    modelRW   = payRW,
    lkCartApi = lkCartApi,
  )

  override protected val actionHandler: HandlerFunction = { (mroot, action) =>
    val handler: HandlerFunction = action match {
      case _: IBillConfAction           => billConfAh
      case _: IOrderItemsAction         => orderItemsAh
      case _: ICartPayAction            => cartPayAh
    }
    handler( mroot, action )
  }


  // constructor(): After, execute HTTP-request about current Cart's Order.
  Future {
    dispatch( LoadCurrentOrder )
  }

}


/** Circuit implementation for LK (private cabinet)'s HTML Cart-page with inlined initial Form state. */
final class CartPageCircuit extends CartCircuitBase {

  override protected def initialModel: MCartRootS =
    CartCircuit.mkInitialModelFromInput()

}
