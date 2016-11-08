package io.suggest.maps.c

import io.suggest.maps.vm.img.{IconVmStaticT, MarkerIcon, MarkerIconRetina, MarkerIconShadow}
import io.suggest.sjs.leaflet.map.{LMap, LatLng}
import io.suggest.sjs.leaflet.marker.{Marker, MarkerOptions}
import io.suggest.sjs.leaflet.marker.icon.{Icon, IconOptions}
import io.suggest.sjs.leaflet.{Leaflet => L}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.16 16:53
  * Description: Поддержка статически-описываемых маркеров на картах leaflet.
  * Статически значит, что картинки определены в тегах на странице.
  */
trait LeafletStaticMarkersUtil {

  def _markerIconBase(iconModel: IconVmStaticT, o: IconOptions = IconOptions.empty): IconOptions = {
    // Собираем обычную иконку маркера.
    for (img <- iconModel.find()) {
      for (src <- img.srcOpt) {
        o.iconUrl = src
      }
      for (wh <- img.wh) {
        o.iconSize = L.point(wh.width, y = wh.height)
      }
      for (xy <- img.xy) {
        o.iconAnchor = L.point(xy.x, y = xy.y)
      }
    }
    o
  }

  protected def _mkMarker(latLng: LatLng, icon: Icon): Marker = {
    val options = MarkerOptions.empty
    options.icon = icon
    options.draggable = true
    L.marker(latLng, options)
  }

}


trait LeafletPinMarker extends LeafletStaticMarkersUtil {

  /** Сборка иконки маркера. */
  def _pinMarkerIcon(): Icon = {
    // Начинаем собирать параметры иконки.
    val o = _markerIconBase(MarkerIcon)

    // Собираем retina-icon'ку маркера
    for {
      img <- MarkerIconRetina.find()
      src <- img.srcOpt
    } {
      o.iconRetinaUrl = src
    }

    // Собираем тень маркера
    for (img <- MarkerIconShadow.find()) {
      for (src <- img.srcOpt) {
        o.shadowUrl = src
      }
      for (wh <- img.wh) {
        o.shadowSize = L.point(wh.width, y = wh.height)
      }
      for (xy <- img.xy) {
        o.shadowAnchor = L.point(xy.x, y = xy.y)
      }
    }

    // Вернуть итоговый результат.
    L.icon(o)
  }

}
