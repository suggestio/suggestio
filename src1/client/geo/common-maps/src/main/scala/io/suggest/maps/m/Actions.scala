package io.suggest.maps.m

import io.suggest.geo.{IGeoPointField, MGeoPoint}
import io.suggest.sjs.common.spa.DAction

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

