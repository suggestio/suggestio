package io.suggest.maps.m

import io.suggest.adv.info.MNodeAdvInfo
import io.suggest.adv.rcvr.MRcvrPopupResp
import io.suggest.geo.{IGeoPointField, MGeoPoint}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.sjs.common.spa.DAction
import io.suggest.sjs.leaflet.map.Zoom_t

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 22:14
  * Description: Экшены для react-компонентов гео.карт.
  *
  * Трейты без sealed, т.к. они потом гуляют по зависимым формам и circuit'ам.
  */

trait IMapsAction extends DAction

/** Интерфейс для сообщений выставления центра. */
trait ISetMapCenterForPopup extends IMapsAction with IGeoPointField


trait ISetMapCenter extends IMapsAction with IGeoPointField

/** Экшен выставления центра карты на указанную гео-точку. */
case class HandleLocationFound(override val geoPoint: MGeoPoint) extends ISetMapCenter


trait IHandleMapPopupClose extends IMapsAction

/** Команда среагировать на сокрытие произвольного попапа карты на стороне leaflet. */
case object HandleMapPopupClose extends IHandleMapPopupClose



// Rad-события для маркера центра круга
/** Экшен начала таскания центра круга. */
case object RadCenterDragStart extends IMapsAction
/** Экшен модификации центра rad-круга в ходе продолжающегося драггинга. */
case class RadCenterDragging(geoPoint: MGeoPoint) extends IMapsAction with IGeoPointField
/** Экшен завершения перетаскивания rad-круга за его центр. */
case class RadCenterDragEnd(geoPoint: MGeoPoint) extends IMapsAction with IGeoPointField


trait IRadClick extends IMapsAction
/** Экшен клика по центру круга. */
case object RadCenterClick extends IRadClick
case object RadAreaClick extends IRadClick


/** Экшен включения/выключени режима размещения прямо на карте. */
case class RadOnOff(enabled: Boolean) extends IMapsAction

// Rad-события для маркера радиуса круга.
case object RadiusDragStart extends IMapsAction
case class RadiusDragging(geoPoint: MGeoPoint) extends IMapsAction with IGeoPointField
case class RadiusDragEnd(geoPoint: MGeoPoint) extends IMapsAction with IGeoPointField



/** Команда к открытию попапа над гео-шейпом (кружком) по уже существующими размещениям. */
case class OpenAdvGeoExistPopup(itemId: Double, geoPoint: MGeoPoint) extends ISetMapCenterForPopup




/** Экшен запуска инициализации карты маркеров ресиверов. */
case object RcvrMarkersInit extends IMapsAction
/** Экшен выставления указанных recevier-маркеров в состояние. */
case class InstallRcvrMarkers(tryResp: Try[MGeoNodesResp]) extends IMapsAction

case class ReqRcvrPopup(nodeId: String, override val geoPoint: MGeoPoint) extends ISetMapCenterForPopup

/** Экшен успешно декодированного ответа на запрос попапа. */
case class HandleRcvrPopupResp(resp: MRcvrPopupResp) extends IMapsAction
case class HandleRcvrPopupTryResp(resp: Try[MNodeAdvInfo], rrp: ReqRcvrPopup) extends IMapsAction
/** Ошибка запроса по теме попапа. */
case class HandleRcvrPopupError(ex: Throwable) extends IMapsAction



/** Интерфейс для событий изменения zoom'а на карте leaflet. */
trait IMapZoomEnd extends IMapsAction {
  /** Новый уровень zoom'а. */
  def newZoom: Zoom_t
}
/** Дефолтовое событие изменения zoom'а на карте.
  * Это дефолтовая реализация [[IMapZoomEnd]]. */
case class MapZoomEnd( override val newZoom: Zoom_t ) extends IMapZoomEnd



//case class MapMoveEnd( newCenterLL: LatLng ) extends IMapsAction
