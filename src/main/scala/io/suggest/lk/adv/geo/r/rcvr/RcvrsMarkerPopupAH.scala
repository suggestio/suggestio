package io.suggest.lk.adv.geo.r.rcvr

import diode._
import diode.data.Pot
import io.suggest.adv.geo.{MRcvrPopupResp, MRcvrPopupState}
import io.suggest.lk.adv.geo.a.{IAdvGeoFormAction, HandleRcvrPopup, HandleRcvrPopupError, ReqRcvrPopup}
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

class RcvrsMarkerPopupAH[M](api: ILkAdvGeoApi,
                            adIdProxy: ModelRO[String],
                            rcvrsRW: ModelRW[M, Pot[MRcvrPopupResp]])
  extends ActionHandler(rcvrsRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал запуска запроса с сервера содержимого попапа для ресивера.
    case rrp: ReqRcvrPopup =>
      // TODO Проверить содержимое rcvrsRW, может там уже есть правильный ответ, и запрос делать не надо.
      val nextState = value.pending()
      val fx = Effect[IAdvGeoFormAction] {
        api.rcvrPopup(adIdProxy(), nodeId = rrp.nodeId)
          .map { HandleRcvrPopup.apply }
          .recover { case ex: Throwable =>
            HandleRcvrPopupError(ex)
          }
      }
      // Перемещение карты в указанную (текущую) точку идёт в другом action-handler'е: RcvrMarkerOnMapAH.
      updated(nextState, fx)

    // Есть ответ от сервера на запрос попапа, надо закинуть ответ в состояние.
    case hrp: HandleRcvrPopup =>
      // Нужно залить в состояние ответ сервера
      val pot2 = value.ready( hrp.resp )
      updated( pot2 )

    // Среагировать как-то на ошибку выполнения запроса.
    case hre: HandleRcvrPopupError =>
      val pot = value
      LOG.error( ErrorMsgs.UNEXPECTED_RCVR_POPUP_SRV_RESP, hre.ex, pot )
      val pot2 = pot.fail(hre.ex)
      updated(pot2)
  }

}


/** Action handler, меняющий состояние попапа. */
class RcvrMarkerPopupState[M](popStateRW: ModelRW[M, Option[MRcvrPopupState]]) extends ActionHandler(popStateRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case rrp: ReqRcvrPopup =>
      updated( Some(MRcvrPopupState(rrp.nodeId, rrp.geoPoint)) )

    // TODO Очищать состояние при закрытии попапа.
  }
}
