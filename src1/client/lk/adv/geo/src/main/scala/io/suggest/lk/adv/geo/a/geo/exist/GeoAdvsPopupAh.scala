package io.suggest.lk.adv.geo.a.geo.exist

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.lk.adv.geo.m._
import io.suggest.lk.adv.geo.r.ILkAdvGeoApi
import io.suggest.maps.m.{HandleMapPopupClose, MExistGeoPopupS, OpenAdvGeoExistPopup}
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.12.16 15:15
  * Description: Action handler для работы с попапами над текущими размещениями.
  */
class GeoAdvsPopupAh[M](
                         api      : ILkAdvGeoApi,
                         modelRW  : ModelRW[M, MExistGeoPopupS]
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
      val v1 = value.withState {
        Some(op)
      }
      updated(v1, fx)

    // Получен какой-то ответ от сервера по поводу попапа:
    case h: HandleAdvGeoExistPopupResp =>
      val v0 = value
      // Почему-то сравнение через open eq open здесь не сработало, поэтому сравниваем по itemId.
      if ( v0.state.exists(_.itemId ==* h.open.itemId) ) {
        // Поступил ожидаемый ответ сервера. Залить его в состояние.
        val v1 = value.withContent( v0.content.ready(h.resp) )
        updated(v1)

      } else {
        logger.log( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = h.open )
        // Какой-то неактуальный ответ сервера пришёл.
        noChange
      }

    // Реакция на сигнал закрытия попапа, когда он открыт.
    case HandleMapPopupClose if value.state.nonEmpty =>
      val v2 = value.copy(
        content  = Pot.empty,
        state    = None
      )
      updated(v2)

  }

}
