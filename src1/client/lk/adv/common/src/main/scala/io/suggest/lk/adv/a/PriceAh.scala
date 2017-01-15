package io.suggest.lk.adv.a

import diode.{ActionHandler, ActionResult, ModelRW}
import diode.data.Pot
import io.suggest.bill.MGetPriceResp
import io.suggest.lk.adv.m.SetPrice

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.01.17 22:07
  * Description: Diode action handler для ценника.
  */
class PriceAh[M](modelRW: ModelRW[M, Pot[MGetPriceResp]]) extends ActionHandler(modelRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {
    case SetPrice(resp) =>
      updated( value.ready(resp) )
  }

}
