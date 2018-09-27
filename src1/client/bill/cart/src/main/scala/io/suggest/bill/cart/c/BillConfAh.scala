package io.suggest.bill.cart.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.bill.cart.MCartConf
import io.suggest.bill.cart.m.{GetOrderContent, LoadCurrentOrder}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.18 17:13
  * Description: Корневой (config-level?) контроллер.
  */
class BillConfAh[M](
                     modelRW: ModelRW[M, MCartConf]
                   )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Экшен загрузки/перезагрузки текущего ордера. Может быть инициирован юзером.
    case LoadCurrentOrder =>
      val v0 = value
      val fx = Effect.action(
        GetOrderContent(
          orderId = v0.orderId
        )
      )
      effectOnly(fx)

  }

}
