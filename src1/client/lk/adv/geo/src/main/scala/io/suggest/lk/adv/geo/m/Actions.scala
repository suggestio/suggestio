package io.suggest.lk.adv.geo.m

import io.suggest.adv.geo.MGeoAdvExistPopupResp
import io.suggest.adv.info.MNodeAdvInfo
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.geo.json.GjFeature
import io.suggest.maps.m.{IMapsAction, OpenAdvGeoExistPopup}

import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:50
  * Description: Diode-экшены формы lk adv geo.
  */

sealed trait IAdvGeoFormAction extends IMapsAction


/** Экшен замены значения галочки размещения на главном экране. */
case class SetOnMainScreen(checked: Boolean) extends IAdvGeoFormAction


/** Экшен на тему изменения статуса ресивера. */
case class SetRcvrStatus(rcvrKey: RcvrKey, checked: Boolean) extends IAdvGeoFormAction


/** Команда инициализации кружчков и др.фигурок текущего размещния. */
case object CurrGeoAdvsInit extends IAdvGeoFormAction
/** Выставить указанные данные размещения в состояние. */
case class SetCurrGeoAdvs( tryResp: Try[js.Array[GjFeature]] ) extends IAdvGeoFormAction


/** Команда к реакции на полученние попапа над гео-областью. */
case class HandleAdvGeoExistPopupResp(open: OpenAdvGeoExistPopup, resp: MGeoAdvExistPopupResp)
  extends IAdvGeoFormAction



/** Сигнал ответа сервера на запрос информации по узлу. */
case class OpenNodeInfoResp(rcvrKey: RcvrKey, tryRes: Try[MNodeAdvInfo]) extends IAdvGeoFormAction


/** Юзер хочет узнать по-больше о форме гео-размещения. */
case object DocReadMoreClick extends IAdvGeoFormAction
