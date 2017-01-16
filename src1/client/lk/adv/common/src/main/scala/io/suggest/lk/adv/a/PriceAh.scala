package io.suggest.lk.adv.a

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.lk.adv.m.{MPriceS, SetPrice}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.01.17 22:07
  * Description: Diode action handler для ценника.
  */
class PriceAh[M](modelRW: ModelRW[M, MPriceS]) extends ActionHandler(modelRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал о выполнении запроса рассчёта стоимости. TODO Проверять актуальность запроса по timestamp...
    case SetPrice(resp) =>
      val v0 = value
      val v1 = v0.withPriceResp(
        v0.resp.ready( resp )
      )
      updated( v1 )

  }

}
