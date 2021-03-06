package io.suggest.maps.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.common.empty.OptionUtil
import io.suggest.geo.{IGeoPointField, MGeoPoint}
import io.suggest.maps.{HandleLocationFound, HandleMapPopupClose, ISetMapCenterForPopup, MMapS, MapMoveEnd, MapZoomEnd}
import io.suggest.maps.m._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 18:04
  * Description: Diode action handler для экшенов очень общих воздействий на геокарту формы.
  * Экшены вызываются параллельно с остальными экшенами, т.к. эти common-экшены носят интерфейсный характер.
  */
class MapCommonAh[M](
                      mmapRW: ModelRW[M, MMapS]
                    )
  extends ActionHandler(mmapRW)
{

  private def _setMapCenter(ismc: IGeoPointField, v0: MMapS = value) = {
    val mgp = ismc.geoPoint
    _setMapCenterTo( mgp, v0 )
  }
  private def _setMapCenterTo(mgp: MGeoPoint, v0: MMapS = value) = {
    _maybeUpdateStateUsing(mgp, v0) {
      _.withCenterInitReal(
        centerInit = mgp
      )
    }
  }
  private def _maybeUpdateStateUsing(mgp: MGeoPoint, v0: MMapS = value)(f: MMapS => MMapS) = {
    if (v0.center ~= mgp) {
      noChange
    } else {
      updated( f(v0) )
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Карта была перемещена, у неё теперь новый центр.
    case m: MapMoveEnd =>
      val v0 = value
      if (v0.center ~= m.newCenter) {
        noChange
      } else {
        // Обновляем через updateSilent, т.к. не нужны никакие side-эффекты.
        val v2 = (MMapS.centerReal replace Some(m.newCenter))(v0)
        updatedSilent( v2 )
      }

    // Реакция на изменение zoom'а.
    case ze: MapZoomEnd =>
      val v0 = value
      if (ze.newZoom ==* v0.zoom) {
        noChange
      } else {
        val v2 = (MMapS.zoom replace ze.newZoom)(v0)
        updatedSilent( v2 )
      }

    // Сигнал об успешном обнаружении геолокации.
    case hlf: HandleLocationFound =>
      // Выставить в состояние флаг, что больше не требуется принимать сигнал геолокации. Это нужно, чтобы избежать передёргивания карты.
      // TODO Снимать флаг, при повторной геолокации.
      val v0 = value
      if (value.locationFound contains true) {
        noChange
      } else {
        val v2 = (MMapS.locationFound replace OptionUtil.SomeBool.someTrue)(v0)
        _setMapCenter(hlf, v2)
      }

    // Сигнал центровки карты с учетом попапа в указанной точке.
    case ismc: ISetMapCenterForPopup =>
      // TODO Нужно, отценровать карту как-то по-особому, чтобы центр был ниже реального центра.
      _setMapCenter(ismc)

    // Сигнал сокрытия попапа на карте Leaflet.
    case HandleMapPopupClose =>
      // Почему-то бывает, что сообщение о закрытие попапа приходят дважды.
      noChange

  }

}
