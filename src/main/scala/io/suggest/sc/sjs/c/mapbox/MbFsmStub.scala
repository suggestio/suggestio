package io.suggest.sc.sjs.c.mapbox

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.m.mmap.{MNodesSourcesSrv, MapNodesRespTs, MbFsmSd}
import io.suggest.sc.sjs.vm.mapbox.GlMapVm
import io.suggest.sjs.common.fsm.SjsFsm
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.mapbox.gl.event.{EventData, IMapSignalCompanion}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 19:12
  * Description: Черновик для сборки кусков и состояний MapBox FSM.
  */
trait MbFsmStub extends SjsFsm with StateData {

  override type State_t = FsmState
  override type SD = MbFsmSd

  protected def _mapSignalCallbackF(model: IMapSignalCompanion[_]) = {
    {arg: EventData =>
      _sendEventSyncSafe( model(arg) )
    }
  }


  /** Инициировать запрос апдейта карты узлов. */
  def _needUpdateNodesMap(): Unit = {
    val sd0 = _stateData
    for {
      glmap <- sd0.glmap
    } {
      val fut = MNodesSourcesSrv.forMap(glmap)
      _sendFutResBackTimestamped(fut, MapNodesRespTs)
    }
  }

  /** Ресивер для всех состояний. */
  override protected val allStatesReceiver: Receive = {
    val pf: Receive = {
      // Сигнал об ответе сервера по карте узлов
      case respTs: MapNodesRespTs =>
        _handleMapNodesResp(respTs)
    }
    pf.orElse {
      super.allStatesReceiver
    }
  }

  /** Реакция на получение ответа по карте узлов. */
  def _handleMapNodesResp(respTs: MapNodesRespTs): Unit = {
    val sd0 = _stateData
    if (sd0.nodesRespTs.isEmpty || sd0.nodesRespTs.exists(_ < respTs.timestamp)) {
      respTs.result match {
        case Success(mnResp) =>
          _stateData = sd0.copy(
            nodesRespTs = Some(respTs.timestamp)
          )
          GlMapVm( _stateData.glmap.get )
            .updateNodesMapLayers( mnResp.sources )

        case Failure(ex) =>
          error( ErrorMsgs.XHR_UNEXPECTED_RESP, ex )
      }
    } else {
      log( WarnMsgs.ADV_DIRECT_XHR_TS_DROP )
    }
  }

}
