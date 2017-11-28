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
    if ((v0.centerInit ~= mgp) || v0.centerReal.exists(_ ~= mgp)) {
      noChange
    } else {
      //println( "set new loc: " + mgp)
      val v2 = v0.copy(
        centerInit = mgp,
        centerReal = None
      )
      updated( v2 )
    }
  }

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Карта была перемещена, у неё теперь новый центр.
    case m: MapMoveEnd =>
      //println( m )
      val mgp = MapsUtil.latLng2geoPoint( m.newCenterLL )
      _setMapCenterTo( mgp )

    // Реакция на изменение zoom'а.
    case ze: IMapZoomEnd =>
      val v0 = value
      if (ze.newZoom ==* v0.zoom) {
        noChange
      } else {
        val v2 = v0.withZoom( ze.newZoom )
        updated( v2 )
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
