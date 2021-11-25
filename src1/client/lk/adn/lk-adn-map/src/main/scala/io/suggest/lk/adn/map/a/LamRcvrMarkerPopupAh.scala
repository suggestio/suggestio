package io.suggest.lk.adn.map.a

import diode.data.Pot
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.lk.adn.map.m.MLamRcvrs
import io.suggest.lk.adn.map.u.ILkAdnMapApi
import io.suggest.lk.m.NodeInfoPopupClose
import io.suggest.maps.m.MRcvrPopupS
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.maps.{HandleRcvrPopupTryResp, OpenMapRcvr}
import io.suggest.spa.DiodeUtil.Implicits._

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
          geoPoint = rrp.geoPoint
        ))
      )

      updated(v2, fx)


    case m: HandleRcvrPopupTryResp =>
      val v0 = value
      if ( v0.popupState.map(_.nodeId).contains( m.rrp.nodeId ) ) {
        val popResp2 = v0.popupResp.withTry( m.resp )
        val v2 = v0.withPopup(
          resp = popResp2,
          state = v0.popupState
            .filter(_ => m.resp.isSuccess)
        )
        updated(v2)

      } else {
        logger.info( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
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
