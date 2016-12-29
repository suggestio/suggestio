package io.suggest.lk.adv.geo.a.geo.adv

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.lk.adv.geo.a.OpenAdvGeoExistPopup
import io.suggest.lk.adv.geo.m.MGeoAdvs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 15:15
  * Description: Action handler для работы с попапами над текущими размещениями.
  */
class GeoAdvsPopupAh[M](
                        modelRW: ModelRW[M, MGeoAdvs]
                      )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Пришла команда к открытию попапа с данными по какому-то георазмещению.
    case op: OpenAdvGeoExistPopup =>
      println("TODO openPopup at " + op.geoPoint + " for " + op.itemId)
      noChange

  }

}
