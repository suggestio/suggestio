package io.suggest.lk.adv.geo.a.mapf

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.adv.geo.MMapS
import io.suggest.geo.IGeoPointField
import io.suggest.lk.adv.geo.a.{ISetMapCenter, ISetMapCenterForPopup}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 18:04
  * Description: Diode action handler для экшенов очень общих воздействий на геокарту формы.
  * Экшены вызываются параллельно с остальными экшенами, т.к. эти common-экшены носят интерфейсный характер.
  */
class MapCommonAh[M](mapStateRW: ModelRW[M, MMapS]) extends ActionHandler(mapStateRW) {

  private def _setMapCenter(ismc: IGeoPointField) = {
    val v2 = value.withCenter( ismc.geoPoint )
    updated( v2 )
  }

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал "жесткой" центровки карты.
    case ismc: ISetMapCenter =>
      _setMapCenter(ismc)

    // Сигнал центровки карты с учетом попапа в указанной точке.
    case ismc: ISetMapCenterForPopup =>
      // TODO Нужно, отценровать карту как-то по-особому, чтобы центр был ниже реального центра.
      _setMapCenter(ismc)

  }

}
