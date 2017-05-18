package io.suggest.lk.adv.geo.a.geo.exist

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.lk.adv.geo.m._
import io.suggest.lk.adv.geo.r.ILkAdvGeoApi
import io.suggest.maps.m.{HandleMapPopupClose, OpenAdvGeoExistPopup}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 15:15
  * Description: Action handler для работы с попапами над текущими размещениями.
  */
class GeoAdvsPopupAh[M](
                         api: ILkAdvGeoApi,
                         modelRW: ModelRW[M, MGeoAdvs]
                       )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Пришла команда к открытию попапа с данными по какому-то георазмещению.
    case op: OpenAdvGeoExistPopup =>
      // Организовать эффект получения данных с сервера.
      val fx = Effect {
        for (resp <- api.existGeoAdvsShapePopup(op.itemId)) yield {
          HandleAdvGeoExistPopupResp(op, resp)
        }
      }
      // Обновлить текущее состояние данными по открываемому попапу:
      val v1 = value.withPopupState {
        Some(MGeoAdvPopupState(op))
      }
      updated(v1, fx)

    // Получен какой-то ответ от сервера по поводу попапа:
    case h: HandleAdvGeoExistPopupResp =>
      val v0 = value
      // Почему-то сравнение через open eq open здесь не сработало, поэтому сравниваем по itemId.
      if ( v0.popupState.exists(_.open.itemId == h.open.itemId) ) {
        // Поступил ожидаемый ответ сервера. Залить его в состояние.
        val v1 = value.withPopupResp( v0.popupResp.ready(h.resp) )
        updated(v1)

      } else {
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = h.open )
        // Какой-то неактуальный ответ сервера пришёл.
        noChange
      }

    // Реакция на сигнал закрытия попапа, когда он открыт.
    case HandleMapPopupClose if value.popupState.nonEmpty =>
      val v2 = value.copy(
        popupResp  = Pot.empty,
        popupState = None
      )
      updated(v2)

  }

}
