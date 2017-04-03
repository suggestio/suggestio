package io.suggest.lk.adv.geo.a.mapf

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.geo.IGeoPointField
import io.suggest.lk.adv.geo.m._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 18:04
  * Description: Diode action handler для экшенов очень общих воздействий на геокарту формы.
  * Экшены вызываются параллельно с остальными экшенами, т.к. эти common-экшены носят интерфейсный характер.
  */
class MapCommonAh[M](mmapRW: ModelRW[M, MMap]) extends ActionHandler(mmapRW) {

  private def _setMapCenter(ismc: IGeoPointField, v0: MMap = value) = {
    val v2 = v0.withProps(
      v0.props.withCenter(
        ismc.geoPoint
      )
    )
    updated( v2 )
  }

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал об успешном обнаружении геолокации.
    case hlf: HandleLocationFound =>
      // Выставить в состояние флаг, что больше не требуется принимать сигнал геолокации.
      val v2 = value.withLocationFound( Some(true) )
      _setMapCenter(hlf, v2)

    // Сигнал "жесткой" центровки карты.
    case ismc: ISetMapCenter =>
      _setMapCenter(ismc)

    // Сигнал центровки карты с учетом попапа в указанной точке.
    case ismc: ISetMapCenterForPopup =>
      // TODO Нужно, отценровать карту как-то по-особому, чтобы центр был ниже реального центра.
      _setMapCenter(ismc)

    // Сигнал сокрытия попапа на карте Leaflet.
    case _: IHandlePopupClose =>
      // Почему-то бывает, что сообщение о закрытие попапа приходят дважды.
      noChange
  }

}
