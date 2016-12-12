package io.suggest.lk.adn.map.init

import io.suggest.lk.adn.map.vm.FormInputs
import io.suggest.maps.c.{LeafletLocateControlUtil, LeafletMapInit, LeafletPinMarker}
import io.suggest.maps.vm.MapContainer
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.leaflet.event.{Event, Events}
import io.suggest.sjs.leaflet.map.{LMap, LatLng}
import io.suggest.sjs.leaflet.marker.Marker


/** Инициализатор только карты размещения ADN-узла на карте. */
class LkAdnMapInit
  extends Log
  with LeafletMapInit
  with LeafletPinMarker
  with LeafletLocateControlUtil
{

  override val _vm = new FormInputs


  /** Инициализация указанной карты, когда контейнер уже найден. */
  override def initMapOn(rmc: MapContainer): LMap = {
    val lmap = super.initMapOn(rmc)

    val lmapCenter0 = lmap.getCenter()

    // Узнать исходные координаты маркера.
    val latLng = _vm.pin.latLngOpt
      .orElse { _vm._map.latLngOpt }
      .getOrElse {
        LOG.error( ErrorMsgs.MISSING_POINT_0 )
        lmapCenter0
      }

    // Собираем и устанавливаем маркер центра круга:
    val cm = _mkDraggableMarker( latLng, _pinMarkerIcon() )
      .addTo(lmap)

    // При определении текущего местоположения следует перемещать маркер в текущее местоположение юзера.
    _onLocateControl(lmap) { e =>
      cm.setLatLng( e.latLng )
    }

    // После перетаскивания маркера надо его координаты сериализовать в соотв.поля формы.
    val cDragEndF = { e: Event =>
      _vm.pin.setLatLon( cm.getLatLng() )
      onPinNewPosition(cm)
    }
    cm.on3(Events.DRAG_END, cDragEndF)

    // Если pin-маркер далековат от центра карты, то отцентрировать карту по pin-маркеру.
    if ( _isTooFar(lmapCenter0, latLng) ) {
      lmap.panTo(latLng)
    }

    lmap
  }


  /** Дополнительная реакция на перетаскивание маркера в новую точку. */
  def onPinNewPosition(marker: Marker): Unit = {}


  /** Координаты слишком далеко друг от друга? */
  def _isTooFar(l0: LatLng, l1: LatLng): Boolean = {
    def testCoord(coordF: LatLng => Double): Boolean = {
      Math.abs( coordF(l1) - coordF(l0) ) > 0.01
    }
    testCoord(_.lat) || testCoord(_.lng)
  }


}
