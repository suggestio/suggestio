package io.suggest.lk.adv.geo.a.oms

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.lk.adv.geo.m.SetOnMainScreen

/** Action handler для галочки размещения на главном экране. */
class OnMainScreenAh[M](
                         modelRW          : ModelRW[M, Boolean],
                         priceUpdateFx    : Effect
                       )
  extends ActionHandler(modelRW)
{
  override protected def handle: PartialFunction[Any, ActionResult[M]] = {
    case SetOnMainScreen(checked2) =>
      val checked0 = value
      if (checked0 != checked2) {
        updated(checked2, priceUpdateFx)
      } else {
        noChange
      }
  }
}
