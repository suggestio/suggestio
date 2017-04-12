package io.suggest.lk.adv.geo.m

import io.suggest.adv.geo.MGeoAdvExistPopupResp
import io.suggest.adv.rcvr.{MRcvrPopupResp, RcvrKey}
import io.suggest.geo.{IGeoPointField, MGeoPoint}
import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.common.spa.DAction
import io.suggest.sjs.leaflet.marker.Marker

import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:50
  * Description: Diode-экшены формы lk adv geo.
  */

sealed trait IAdvGeoFormAction extends DAction

/** Интерфейс для сообщений выставления центра. */
sealed trait ISetMapCenter extends IAdvGeoFormAction with IGeoPointField
sealed trait ISetMapCenterForPopup extends IAdvGeoFormAction with IGeoPointField


/** Экшен успешно декодированного ответа на запрос попапа. */
case class HandleRcvrPopup(resp: MRcvrPopupResp) extends IAdvGeoFormAction
/** Ошибка запроса по теме попапа. */
case class HandleRcvrPopupError(ex: Throwable) extends IAdvGeoFormAction

case class ReqRcvrPopup(nodeId: String, geoPoint: MGeoPoint) extends ISetMapCenterForPopup

/** Экшен замены значения галочки размещения на главном экране. */
case class SetOnMainScreen(checked: Boolean) extends IAdvGeoFormAction


/** Экшен на тему изменения статуса ресивера. */
case class SetRcvrStatus(rcvrKey: RcvrKey, checked: Boolean) extends IAdvGeoFormAction


/** Экшен запуска инициализации карты маркеров ресиверов. */
case object RcvrMarkersInit extends IAdvGeoFormAction

/** Экшен выставления указанных recevier-маркеров в состояние. */
case class InstallRcvrMarkers(rcvrMarkers: js.Array[Marker]) extends IAdvGeoFormAction


/** Экшен выставления центра карты на указанную гео-точку. */
case class HandleLocationFound(override val geoPoint: MGeoPoint) extends ISetMapCenter


/** Команда инициализации кружчков и др.фигурок текущего размещния. */
case object CurrGeoAdvsInit extends IAdvGeoFormAction
/** Выставить указанные данные размещения в состояние. */
case class SetCurrGeoAdvs(resp: js.Array[GjFeature]) extends IAdvGeoFormAction


/** Команда к открытию попапа над гео-шейпом (кружком) по уже существующими размещениям. */
case class OpenAdvGeoExistPopup(itemId: Double, geoPoint: MGeoPoint) extends ISetMapCenterForPopup

/** Команда к реакции на полученние попапа над гео-областью. */
case class HandleAdvGeoExistPopupResp(open: OpenAdvGeoExistPopup, resp: MGeoAdvExistPopupResp)
  extends IAdvGeoFormAction


sealed trait IHandlePopupClose extends IAdvGeoFormAction
/** Команда среагировать на сокрытие произвольного попапа карты на стороне leaflet. */
case object HandlePopupClose extends IHandlePopupClose


// Rad-события для маркера центра круга
/** Экшен начала таскания центра круга. */
case object RadCenterDragStart extends IAdvGeoFormAction
/** Экшен модификации центра rad-круга в ходе продолжающегося драггинга. */
case class RadCenterDragging(geoPoint: MGeoPoint) extends IAdvGeoFormAction with IGeoPointField
/** Экшен завершения перетаскивания rad-круга за его центр. */
case class RadCenterDragEnd(geoPoint: MGeoPoint) extends IAdvGeoFormAction with IGeoPointField
/** Экшен клика по центру круга. */
case object RadClick extends IAdvGeoFormAction


/** Экшен включения/выключени режима размещения прямо на карте. */
case class RadOnOff(enabled: Boolean) extends IAdvGeoFormAction

// Rad-события для маркера радиуса круга.
case object RadiusDragStart extends IAdvGeoFormAction
case class RadiusDragging(geoPoint: MGeoPoint) extends IAdvGeoFormAction with IGeoPointField
case class RadiusDragEnd(geoPoint: MGeoPoint) extends IAdvGeoFormAction with IGeoPointField


/** Сигнал открытия инфы по узлу. */
case class OpenNodeInfoClick(rcvrKey: RcvrKey) extends IAdvGeoFormAction
/** Сигнал ответа сервера на запрос информации по узлу. */
case class OpenNodeInfoResp(rcvrKey: RcvrKey, tryRes: Try[String]) extends IAdvGeoFormAction
