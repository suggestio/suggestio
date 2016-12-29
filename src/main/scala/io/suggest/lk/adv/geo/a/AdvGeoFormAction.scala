package io.suggest.lk.adv.geo.a

import io.suggest.adv.geo.{MRcvrPopupResp, RcvrKey}
import io.suggest.geo.{IGeoPointField, MGeoPoint}
import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.common.spa.DAction
import io.suggest.sjs.leaflet.marker.Marker

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:50
  * Description: Diode-экшены формы lk adv geo.
  */

sealed trait AdvGeoFormAction extends DAction

/** Интерфейс для сообщений выставления центра. */
sealed trait ISetMapCenter extends AdvGeoFormAction with IGeoPointField
sealed trait ISetMapCenterForPopup extends AdvGeoFormAction with IGeoPointField


/** Экшен успешно декодированного ответа на запрос попапа. */
case class HandleRcvrPopup(resp: MRcvrPopupResp) extends AdvGeoFormAction
/** Ошибка запроса по теме попапа. */
case class HandleRcvrPopupError(ex: Throwable) extends AdvGeoFormAction

case class ReqRcvrPopup(nodeId: String, geoPoint: MGeoPoint) extends ISetMapCenterForPopup

/** Экшен замены значения галочки размещения на главном экране. */
case class SetOnMainScreen(checked: Boolean) extends AdvGeoFormAction


/** Экшен на тему изменения статуса ресивера. */
case class SetRcvrStatus(rcvrKey: RcvrKey, checked: Boolean) extends AdvGeoFormAction


/** Экшен запуска инициализации карты маркеров ресиверов. */
case object RcvrMarkersInit extends AdvGeoFormAction

/** Экшен выставления указанных recevier-маркеров в состояние. */
case class InstallRcvrMarkers(rcvrMarkers: js.Array[Marker]) extends AdvGeoFormAction


/** Экшен выставления центра карты на указанную гео-точку. */
case class SetMapCenter(override val geoPoint: MGeoPoint) extends ISetMapCenter


/** Выставить новое значение стоимости размещения. */
case class SetPrice(price: String) extends AdvGeoFormAction


/** Команда инициализации кружчков и др.фигурок текущего размещния. */
case object CurrGeoAdvsInit extends AdvGeoFormAction
/** Выставить указанные данные размещения в состояние. */
case class SetCurrGeoAdvs(resp: js.Array[GjFeature]) extends AdvGeoFormAction


/** Команда к открытию попапа над гео-шейпом (кружком) по уже существующими размещениям. */
case class OpenAdvGeoExistPopup(itemId: Double, geoPoint: MGeoPoint) extends ISetMapCenterForPopup
