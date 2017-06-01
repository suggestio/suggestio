package io.suggest.lk.adv.geo.m

import io.suggest.adv.geo.MGeoAdvExistPopupResp
import io.suggest.adv.info.MNodeAdvInfo
import io.suggest.adv.rcvr.{MRcvrPopupResp, RcvrKey}
import io.suggest.geo.MGeoPoint
import io.suggest.maps.m.{IMapsAction, ISetMapCenterForPopup, OpenAdvGeoExistPopup}
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.sjs.common.geo.json.{BooGjFeature, GjFeature}

import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:50
  * Description: Diode-экшены формы lk adv geo.
  */

sealed trait IAdvGeoFormAction extends IMapsAction


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
case class InstallRcvrMarkers(tryResp: Try[ Seq[BooGjFeature[MAdvGeoMapNodeProps]] ]) extends IAdvGeoFormAction


/** Команда инициализации кружчков и др.фигурок текущего размещния. */
case object CurrGeoAdvsInit extends IAdvGeoFormAction
/** Выставить указанные данные размещения в состояние. */
case class SetCurrGeoAdvs(resp: js.Array[GjFeature]) extends IAdvGeoFormAction


/** Команда к реакции на полученние попапа над гео-областью. */
case class HandleAdvGeoExistPopupResp(open: OpenAdvGeoExistPopup, resp: MGeoAdvExistPopupResp)
  extends IAdvGeoFormAction




/** Сигнал открытия инфы по узлу. */
case class OpenNodeInfoClick(rcvrKey: RcvrKey) extends IAdvGeoFormAction
/** Сигнал ответа сервера на запрос информации по узлу. */
case class OpenNodeInfoResp(rcvrKey: RcvrKey, tryRes: Try[MNodeAdvInfo]) extends IAdvGeoFormAction


/** Юзер хочет узнать по-больше о форме гео-размещения. */
case object DocReadMoreClick extends IAdvGeoFormAction
