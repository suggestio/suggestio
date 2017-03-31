package io.suggest.lk.adv.geo.a.rcvr

import diode._
import diode.data.Pot
import io.suggest.lk.adv.geo.m.{InstallRcvrMarkers, RcvrMarkersInit}
import io.suggest.lk.adv.geo.r.ILkAdvGeoApi
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.leaflet.marker.Marker

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.12.16 16:36
  * Description: Diode action handler для инициализации карты ресиверов.
  */
class RcvrMarkersInitAh[M](
                            api: ILkAdvGeoApi,
                            adIdProxy: ModelRO[String],
                            modelRW: ModelRW[M, Pot[js.Array[Marker]]]
                          )
  extends ActionHandler(modelRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал запуска инициализации маркеров с сервера.
    case RcvrMarkersInit =>
      val fx = Effect {
        for {
          resp <- api.rcvrsMap(adIdProxy())
        } yield {
          // Привести результат к js.Array[Markers].
          val markersArr = LkAdvGeoFormUtil.geoJsonToClusterMarkers(resp.featuresIter)
          InstallRcvrMarkers( markersArr )
        }
      }
      updated( value.pending(), fx )

    // Результат реквеста карты маркеров пришёл и готов к заливке в карту.
    case InstallRcvrMarkers(markersArr) =>
      updated( value.ready(markersArr) )

  }
}
