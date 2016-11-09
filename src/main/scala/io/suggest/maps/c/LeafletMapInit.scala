package io.suggest.maps.c

import io.suggest.maps.vm.MapContainer
import io.suggest.maps.vm.inp.MapStateInputs
import io.suggest.sjs.common.controller.IInit
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.util.ISjsLogger
import io.suggest.sjs.leaflet.event.{Event, Events}
import io.suggest.sjs.leaflet.map.LMap
import io.suggest.sjs.leaflet.tilelayer.TlOptions
import io.suggest.sjs.leaflet.{Leaflet => L}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.16 16:20
  * Description: Трейт для инициализации leaflet-карты в указанном контейнере.
  */
trait LeafletMapInit extends ISjsLogger with IInit {

  /** Выполнить инициализцию карты. */
  override def init(): Unit = {
    MapContainer
      .find()
      .fold[Any] {
        warn(WarnMsgs.RAD_MAP_CONT_MISSING)
      } {
        initMapOn
      }
  }

  def _vm: MapStateInputs

  /** Инициализация указанной карты, когда контейнер уже найден. */
  def initMapOn(rmc: MapContainer): LMap = {
    // Инициализировать контейнер карты.
    val lmap = L.map(rmc._underlying)
    _earlyInitMap(lmap)

    // Инициализировать слой OSM
    val tlOpts = TlOptions.empty
    tlOpts.attribution = """&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors"""
    tlOpts.detectRetina = true
    L.tileLayer("http://{s}.tile.osm.org/{z}/{x}/{y}.png", tlOpts)
      .addTo(lmap)

    // После таскания карты надо сохранять новый центр карты в форму.
    val mapDragEndF = { e: Event =>
      _vm._map.setLatLon(
        lmap.getCenter()
      )
    }
    lmap.on3(Events.DRAG_END, mapDragEndF)

    // При изменении зума надо обновлять form-поле zoom'а.
    val zoomEndF = { e: Event =>
      for (zoomInp <- _vm._map.zoom) {
        zoomInp.value = lmap.getZoom()
      }
    }
    lmap.on3(Events.ZOOM_END, zoomEndF)
  }


  /** Провести собственную инициализацию карты на основе моделей инпутов map state. */
  def _earlyInitMap(lmap: LMap): LMap = {
    val _m = _vm._map
    val r = for {
      latLng  <- _m.latLngOpt
      inpZoom <- _m.zoom
      zoom    <- inpZoom.value
    } yield {
      lmap.setView(
        center = latLng,
        zoom   = zoom
      )
    }
    // Если с инициализацией не фартануло, то передать исполнение на super-инициализацию.
    if (r.isEmpty) {
      warn( WarnMsgs.RAD_MAP_NO_START_STATE )
      _earlyInitMapFallback(lmap)
    }
    // Вернуть исходную карту, чтобы можно было override'ить результат работы.
    lmap
  }

  // Дефолтовые координаты: Кремль, Мск
  protected def _dfltLatLng = L.latLng(55.75087, 37.61869)

  /** Ранняя инициализация карты, т.е. ещё до всех слоёв.
    * Нужно обязательно выставить zoom и начальный центр. */
  def _earlyInitMapFallback(lmap: LMap): Unit = {
    lmap.setView(
      center  = _dfltLatLng,
      zoom    = 10
    )
  }

}
