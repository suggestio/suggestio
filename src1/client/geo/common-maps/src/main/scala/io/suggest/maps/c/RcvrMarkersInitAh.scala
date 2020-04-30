package io.suggest.maps.c

import diode._
import diode.data.Pot
import io.suggest.maps.m.{InstallRcvrMarkers, RcvrMarkersInit}
import io.suggest.maps.nodes.{MGeoNodesResp, MRcvrsMapUrlArgs}
import io.suggest.maps.u.IAdvRcvrsMapApi
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 16:36
  * Description: Утиль для контроллера [[RcvrMarkersInitAh]].
  */
object RcvrMarkersInitAh {

  /** Запуск инициализации карты. */
  def startInitFx(args: MRcvrsMapUrlArgs, api: IAdvRcvrsMapApi): Effect = {
    Effect {
      api.advRcvrsMapJson(args)
        .transform { tryResp =>
          val r = InstallRcvrMarkers( tryResp )
          Success( r )
        }
    }
  }

}


/** Diode action handler для обслуживания карты ресиверов. */
class RcvrMarkersInitAh[M](
                            api       : IAdvRcvrsMapApi,
                            argsRO    : ModelRO[MRcvrsMapUrlArgs],
                            modelRW   : ModelRW[M, Pot[MGeoNodesResp]]
                          )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал запуска инициализации маркеров с сервера.
    case RcvrMarkersInit =>
      val fx = RcvrMarkersInitAh.startInitFx( argsRO.value, api )
      // silent, т.к. RcvrMarkersR работает с этим Pot как с Option, а больше это никого и не касается.
      updatedSilent( value.pending(), fx )


    // Результат реквеста карты маркеров пришёл и готов к заливке в карту.
    case m: InstallRcvrMarkers =>
      val v2 = m.tryResp.fold(
        {ex =>
          logger.error( ErrorMsgs.INIT_RCVRS_MAP_FAIL, msg = m, ex = ex )
          value.fail(ex)
        },
        value.ready
      )
      updated( v2 )

  }

}
