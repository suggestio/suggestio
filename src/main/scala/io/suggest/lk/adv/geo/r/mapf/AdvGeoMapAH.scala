package io.suggest.lk.adv.geo.r.mapf

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.adv.geo.MMapS
import io.suggest.lk.adv.geo.a.SetMapCenter

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 18:04
  * Description: Diode action handler для экшенов воздействия на геокарту формы.
  */
class AdvGeoMapAH[M](mapStateRW: ModelRW[M, MMapS]) extends ActionHandler(mapStateRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case SetMapCenter(gp) =>
      val v2 = mapStateRW().withCenter(gp)
      updated( v2 )

  }

}
