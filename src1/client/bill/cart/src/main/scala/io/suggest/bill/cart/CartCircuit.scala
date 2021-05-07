package io.suggest.bill.cart

import diode.react.ReactConnector
import io.suggest.bill.cart.c.{BillConfAh, ILkCartApi, LkCartApiXhrImpl, OrderItemsAh}
import io.suggest.bill.cart.m.{LoadCurrentOrder, MCartRootS, MOrderItemsS}
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
  private val orderRW = CircuitUtil.mkLensRootZoomRW(this, MCartRootS.order)( MOrderItemsS.MOrderItemsSFastEq )
  private val confRW = CircuitUtil.mkLensRootZoomRW(this, MCartRootS.conf)


  // APIs for controllers
  val lkCartApi: ILkCartApi = new LkCartApiXhrImpl


  // Controllers
  private val orderItemsAh = new OrderItemsAh(
    lkCartApi   = lkCartApi,
    modelRW     = orderRW
  )

  private val billConfAh = new BillConfAh(
    modelRW = confRW
  )

  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      orderItemsAh,
      billConfAh
    )
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
