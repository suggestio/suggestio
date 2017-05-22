package io.suggest.lk.adn.map.a

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.maps.m.{HandleMapPopupClose, IRadClick}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.17 17:42
  * Description: Action handler для обслуживание L-попапа на rad-элементами.
  */
class LamRadPopupAh[M](modelRW: ModelRW[M, Boolean])
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по rad-элементам на карте
    case _: IRadClick =>
      val v0 = value
      if (v0)
        noChange
      else
        updated(true)

    // Закрытие попапа над rad-элементами.
    case HandleMapPopupClose if value =>
      updated(false)

  }

}
