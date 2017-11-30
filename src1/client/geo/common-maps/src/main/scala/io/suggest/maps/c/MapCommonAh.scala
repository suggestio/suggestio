package io.suggest.maps.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.geo.{IGeoPointField, MGeoPoint}
import io.suggest.maps.m._
import io.suggest.maps.u.MapsUtil
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 18:04
  * Description: Diode action handler для экшенов очень общих воздействий на геокарту формы.
  * Экшены вызываются параллельно с остальными экшенами, т.к. эти common-экшены носят интерфейсный характер.
  */
class MapCommonAh[M](mmapRW: ModelRW[M, MMapS]) extends ActionHandler(mmapRW) {

  private def _setMapCenter(ismc: IGeoPointField, v0: MMapS = value) = {
    val mgp = ismc.geoPoint
    _setMapCenterTo( mgp, v0 )
  }
  private def _setMapCenterTo(mgp: MGeoPoint, v0: MMapS = value) = {
    _maybeUpdateStateUsing(mgp, v0) {
      _.copy(
        centerInit = mgp,
        centerReal = None
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


  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Карта была перемещена, у неё теперь новый центр.
    case m: MapMoveEnd =>
      val mgp = MapsUtil.latLng2geoPoint( m.newCenterLL )
      val v0 = value
      if (v0.center ~= mgp) {
        noChange
      } else {
        // Обновляем через updateSilent, т.к. не нужны никакие side-эффекты.
        val v2 = v0.withCenterReal( Some(mgp) )
        updatedSilent( v2 )
      }

    // Реакция на изменение zoom'а.
    case ze: IMapZoomEnd =>
      val v0 = value
      if (ze.newZoom ==* v0.zoom) {
        noChange
      } else {
        val v2 = v0.withZoom( ze.newZoom )
        updatedSilent( v2 )
      }

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
    case _: IHandleMapPopupClose =>
      // Почему-то бывает, что сообщение о закрытие попапа приходят дважды.
      noChange

  }

}
