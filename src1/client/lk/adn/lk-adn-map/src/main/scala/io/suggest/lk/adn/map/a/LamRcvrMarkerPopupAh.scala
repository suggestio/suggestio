package io.suggest.lk.adn.map.a

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.adv.rcvr.MRcvrPopupS
import io.suggest.lk.adn.map.m.MLamRcvrs
import io.suggest.lk.adn.map.u.ILkAdnMapApi
import io.suggest.lk.m.NodeInfoPopupClose
import io.suggest.maps.m.{HandleMapPopupClose, HandleRcvrPopupTryResp, OpenMapRcvr}
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.06.17 16:11
  * Description:
  */
class LamRcvrMarkerPopupAh[M](
                               api      : ILkAdnMapApi,
                               rcvrsRW  : ModelRW[M, MLamRcvrs]
                             )
  extends ActionHandler( rcvrsRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал запуска запроса с сервера содержимого попапа для ресивера.
    case rrp: OpenMapRcvr =>
      val fx = Effect {
        api
          .nodeAdvInfo( rrp.nodeId )
          .transform { tryResp =>
            val msg = HandleRcvrPopupTryResp(
              resp = tryResp,
              rrp  = rrp
            )
            Success( msg )
          }
      }
      val v0 = value
      val v2 = v0.withPopup(
        resp = v0.popupResp.pending(),
        state = Some( MRcvrPopupS(
          nodeId = rrp.nodeId,
          latLng = rrp.geoPoint
        ))
      )

      updated(v2, fx)


    case m: HandleRcvrPopupTryResp =>
      val v0 = value
      if ( v0.popupState.map(_.nodeId).contains( m.rrp.nodeId ) ) {
        val popResp2 = m.resp.fold(
          v0.popupResp.fail,
          v0.popupResp.ready
        )
        val v2 = v0.withPopup(
          resp = popResp2,
          state = v0.popupState
            .filter(_ => m.resp.isSuccess)
        )
        updated(v2)

      } else {
        LOG.info( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }

    case NodeInfoPopupClose if value.popupState.nonEmpty || value.popupResp.nonEmpty =>
      val v2 = value.withPopup(
        resp  = Pot.empty,
        state = None
      )
      updated(v2)

  }

}
