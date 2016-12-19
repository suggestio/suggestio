package io.suggest.lk.adv.geo.a

import io.suggest.adv.geo.{MRcvrPopupResp, RcvrKey}
import io.suggest.geo.MGeoPoint
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


/** Экшен успешно декодированного ответа на запрос попапа. */
case class HandleRcvrPopup(resp: MRcvrPopupResp) extends AdvGeoFormAction
/** Ошибка запроса по теме попапа. */
case class HandleRcvrPopupError(ex: Throwable) extends AdvGeoFormAction

case class ReqRcvrPopup(nodeId: String, geoPoint: MGeoPoint) extends AdvGeoFormAction

/** Экшен замены значения галочки размещения на главном экране. */
case class SetOnMainScreen(checked: Boolean) extends AdvGeoFormAction


/** Экшен на тему изменения статуса ресивера. */
case class SetRcvrStatus(rcvrKey: RcvrKey, checked: Boolean) extends AdvGeoFormAction


/** Экшен запуска инициализации карты маркеров ресиверов. */
case object LetsInitRcvrMarkers extends AdvGeoFormAction

/** Экшен выставления указанных recevier-маркеров в состояние. */
case class InstallRcvrMarkers(rcvrMarkers: js.Array[Marker]) extends AdvGeoFormAction


/** Экшен выставления центра карты на указанную гео-точку. */
case class SetMapCenter(gp: MGeoPoint) extends AdvGeoFormAction
