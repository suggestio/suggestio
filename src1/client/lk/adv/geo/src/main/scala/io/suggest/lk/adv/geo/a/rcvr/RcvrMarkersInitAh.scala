package io.suggest.lk.adv.geo.a.rcvr

import diode._
import diode.data.Pot
import io.suggest.lk.adv.geo.m.{InstallRcvrMarkers, RcvrMarkersInit}
import io.suggest.lk.adv.geo.r.ILkAdvGeoApi
import io.suggest.maps.u.MapIcons
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.leaflet.marker.Marker

import scala.scalajs.js
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 16:36
  * Description: Diode action handler для инициализации карты ресиверов.
  */
class RcvrMarkersInitAh[M](
                            api       : ILkAdvGeoApi,
                            adIdProxy : ModelRO[String],
                            modelRW   : ModelRW[M, Pot[js.Array[Marker]]]
                          )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал запуска инициализации маркеров с сервера.
    case RcvrMarkersInit =>
      val fx = Effect {
        api.rcvrsMap( adIdProxy() )
          .transform { tryResp =>
            val r = InstallRcvrMarkers( tryResp )
            Success( r )
          }
      }
      updated( value.pending(), fx )


    // Результат реквеста карты маркеров пришёл и готов к заливке в карту.
    case m: InstallRcvrMarkers =>
      val v2 = m.tryResp.fold(
        {ex =>
          LOG.error( ErrorMsgs.INIT_RCVRS_MAP_GJ_REQ_FAIL, ex)
          value.fail( ex )
        },
        {resp =>
          // Привести результат к js.Array[Markers].
          val markersArr = MapIcons.geoJsonToClusterMarkers( resp )
          value.ready(markersArr)
        }
      )
      updated( v2 )

  }
}
