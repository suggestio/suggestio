package io.suggest.sc.u

import diode.Dispatcher
import io.suggest.sc.m.dev.GlLeafletLocateArgs
import io.suggest.sc.m.GlLeafletApiLocate
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.map.{LMap, LocateOptions}
import org.scalajs.dom.{Position, PositionError}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.10.2020 14:28
  * Description: На cordova недоступно HTML5 Geolocation API в связи с использование плагина bg-geolocation.
  * Внутри методов Leaflet.Map.prototype.locate() и stopLocate() используется HTML5 Geolocation API, и переопределить
  * это можно только патчингом этой самой Map, чтобы данные вызовы из leaflet и плагинов пробрасывались в circuit.
  *
  * Плагин LocateControl использует данное API.
  */
class Sc3LeafletOverrides(
                           dispatcher: => Dispatcher,
                         ) {

  /** Замена locate/stopLocate-функций в L.Map, чтобы запросы локации пробрасывались в контроллеры выдачи. */
  def mapPatch(): Unit = {
    val LL = Leaflet.asInstanceOf[js.Dynamic]
    LL.Map = LL.Map.extend(js.Dictionary[js.Any](

      // Запуск геолокации.
      "locate" -> ({ (that: LMap, options0: LocateOptions) =>
        // Дополняем опции дефолтовыми параметрами, как внутри leaflet: https://github.com/Leaflet/Leaflet/blob/master/src/map/Map.js#L640
        val options2 = new LocateOptions {
          override val watch = js.defined( options0.watch getOrElse false )
          override val setView = options0.setView
          override val maxZoom = options0.maxZoom
          override val timeout = js.defined { options0.timeout getOrElse 10000 }
          override val maximumAge = options0.maximumAge
          override val enableHighAccuracy = options0.enableHighAccuracy
        }
        that._locateOptions = options2
        val action = GlLeafletApiLocate( Some {
          GlLeafletLocateArgs(
            locateOpts = options2,
            onLocation = ( that._handleGeolocationResponse: js.Function1[Position, Unit] )
              .bind(that)
              .asInstanceOf[js.Function1[Position, Unit]],
            onLocError = ( that._handleGeolocationError: js.Function1[PositionError, Unit] )
              .bind(that)
              .asInstanceOf[js.Function1[PositionError, Unit]],
          )
        })
        dispatcher.dispatch( action )
      }: js.ThisFunction),

      // Остановка watching'а.
      "stopLocate" -> ({ (that: LMap) =>
        val action = GlLeafletApiLocate( None )
        dispatcher.dispatch( action )
      }: js.ThisFunction),

    ))
  }

}
