package io.suggest.bill.cart.c

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.bill.cart.MCartConf
import io.suggest.bill.cart.m.{GetOrderContent, LoadCurrentOrder}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.18 17:13
  * Description: Cart config controller.
  */
class BillConfAh[M](
                     modelRW: ModelRW[M, MCartConf]
                   )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Action for (re)loading current order.
    case LoadCurrentOrder =>
      val v0 = value
      val fx = GetOrderContent( orderId = v0.orderId )
        .toEffectPure
      effectOnly(fx)

  }

}
