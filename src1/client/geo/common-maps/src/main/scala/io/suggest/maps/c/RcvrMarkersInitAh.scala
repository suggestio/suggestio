package io.suggest.maps.c

import diode._
import diode.data.Pot
import io.suggest.lk.router.IStaticApi
import io.suggest.maps.m.{InstallRcvrMarkers, RcvrMarkersInit}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 16:36
  * Description: Diode action handler для инициализации карты ресиверов.
  */
class RcvrMarkersInitAh[M](
                            api       : IStaticApi,
                            modelRW   : ModelRW[M, Pot[MGeoNodesResp]]
                          )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал запуска инициализации маркеров с сервера.
    case RcvrMarkersInit =>
      val fx = Effect {
        api.advRcvrsMap()
          .transform { tryResp =>
            val r = InstallRcvrMarkers( tryResp )
            Success( r )
          }
      }
      updated( value.pending(), fx )


    // Результат реквеста карты маркеров пришёл и готов к заливке в карту.
    case m: InstallRcvrMarkers =>
      val v2 = m.tryResp.fold(
        value.fail,
        value.ready
      )
      updated( v2 )

  }

}
