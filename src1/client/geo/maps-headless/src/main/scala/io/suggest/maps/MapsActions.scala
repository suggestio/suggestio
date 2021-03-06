package io.suggest.maps

import diode.data.Pot
import io.suggest.adv.info.MNodeAdvInfo
import io.suggest.adv.rcvr.MRcvrPopupResp
import io.suggest.geo.{IGeoPointField, MGeoPoint}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.spa.DAction
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

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
sealed trait ISetMapCenterForPopup extends IMapsAction with IGeoPointField


/** Экшен выставления центра карты на указанную гео-точку. */
case class HandleLocationFound(override val geoPoint: MGeoPoint) extends IMapsAction with IGeoPointField


/** Команда среагировать на сокрытие произвольного попапа карты на стороне leaflet. */
case object HandleMapPopupClose extends IMapsAction


// Rad-события для маркера центра круга
/** Экшен начала таскания центра круга. */
case object RadCenterDragStart extends IMapsAction
/** Экшен модификации центра rad-круга в ходе продолжающегося драггинга. */
case class RadCenterDragging(geoPoint: MGeoPoint) extends IMapsAction with IGeoPointField
/** Экшен завершения перетаскивания rad-круга за его центр. */
case class RadCenterDragEnd(geoPoint: MGeoPoint) extends IMapsAction with IGeoPointField


sealed trait IRadClick extends IMapsAction
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
object OpenAdvGeoExistPopup {
  @inline implicit def univEq: UnivEq[OpenAdvGeoExistPopup] = UnivEq.derive
}


sealed trait IRcvrMarkersInitAction extends DAction

/** Экшен запуска инициализации карты маркеров ресиверов. */
case class RcvrMarkersInit( resp: Pot[MGeoNodesResp] = Pot.empty ) extends IRcvrMarkersInitAction
object RcvrMarkersInit {
  def resp = GenLens[RcvrMarkersInit]( _.resp )
}

/** Экшен "открытия" ресивера на карте.
  * В редакторе - открытие попапа. В выдаче - переход в ресивер.
  */
case class OpenMapRcvr(nodeId: String, override val geoPoint: MGeoPoint) extends ISetMapCenterForPopup

/** Экшен успешно декодированного ответа на запрос попапа. */
case class HandleRcvrPopupResp(resp: MRcvrPopupResp) extends IMapsAction
case class HandleRcvrPopupTryResp(resp: Try[MNodeAdvInfo], rrp: OpenMapRcvr) extends IMapsAction
/** Ошибка запроса по теме попапа. */
case class HandleRcvrPopupError(ex: Throwable) extends IMapsAction



/** событие изменения zoom'а на карте. */
case class MapZoomEnd( newZoom: Int ) extends IMapsAction


/** Сигнал перемещения центра карты в новую гео-точку.
  *
  * @param newCenter Гео.координата нового центра карты.
  */
case class MapMoveEnd( newCenter: MGeoPoint ) extends IMapsAction


/** Событие окончания перетаскивания карты. */
case class MapDragEnd(distancePx: Double) extends IMapsAction
