package io.suggest.lk.adv.geo.a.rcvr

import diode._
import diode.data.Pot
import io.suggest.adv.rcvr.MRcvrPopupS
import io.suggest.lk.adv.geo.a._
import io.suggest.lk.adv.geo.m._
import io.suggest.lk.adv.geo.r.ILkAdvGeoApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 13:05
  * Description: Action handler для карты ресиверов.
  */

class RcvrsMarkerPopupAh[M](api: ILkAdvGeoApi,
                            adIdProxy: ModelRO[String],
                            rcvrsRW: ModelRW[M, MRcvr])
  extends ActionHandler(rcvrsRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал запуска запроса с сервера содержимого попапа для ресивера.
    case rrp: ReqRcvrPopup =>
      // TODO Проверить содержимое rcvrsRW, может там уже есть правильный ответ, и запрос делать не надо.
      val fx = Effect[IAdvGeoFormAction] {
        api.rcvrPopup(adIdProxy(), nodeId = rrp.nodeId)
          .map { HandleRcvrPopup.apply }
          .recover { case ex: Throwable =>
            HandleRcvrPopupError(ex)
          }
      }
      val v0 = value
      // Перемещение карты в указанную (текущую) точку в maps common-Ah
      // Выставить состояние popup'а.
      val v2 = v0.withPopup(
        resp  = v0.popupResp.pending(),
        state = Some(MRcvrPopupS(rrp.nodeId, rrp.geoPoint))
      )
      updated(v2, fx)

    // Есть ответ от сервера на запрос попапа, надо закинуть ответ в состояние.
    case hrp: HandleRcvrPopup =>
      // Нужно залить в состояние ответ сервера
      val v0 = value
      val v2 = v0.withPopupResp(
        v0.popupResp.ready( hrp.resp )
      )
      updated( v2 )

    // Среагировать как-то на ошибку выполнения запроса.
    case hre: HandleRcvrPopupError =>
      val v0 = value
      LOG.error( ErrorMsgs.UNEXPECTED_RCVR_POPUP_SRV_RESP, hre.ex, v0.popupState )
      val v2 = v0.withPopup(
        resp  = v0.popupResp.fail(hre.ex),
        state = None
      )
      updated(v2)

    // Среагировать на закрытие попапа, если он открыт сейчас.
    case HandlePopupClose if value.popupState.nonEmpty =>
      val v2 = value.withPopup(
        resp  = Pot.empty,
        state = None
      )
      updated(v2)

  }

}
