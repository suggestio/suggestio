package io.suggest.maps.u

import io.suggest.common.spa.SpaConst.LkPreLoader
import io.suggest.maps.vm.img.{IconVmStaticT, MarkerIcon, MarkerIconRetina, MarkerIconShadow}
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.marker.{Marker, MarkerOptions}
import io.suggest.sjs.leaflet.marker.icon.{Icon, IconOptions}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.17 15:59
  * Description: Иконки для маркеров на карте.
  */
object MapIcons {

  def mkDraggableMarker(latLng: LatLng, icon1: Icon): Marker = {
    val options = new MarkerOptions {
      override val icon       = icon1
      override val draggable  = true
    }
    Leaflet.marker(latLng, options)
  }


  def markerIconBase(iconModel: IconVmStaticT, o: IconOptions = IconOptions.empty): IconOptions = {
    // Собираем обычную иконку маркера.
    for (img <- iconModel.find()) {
      for (src <- img.srcOpt) {
        o.iconUrl = src
      }
      for (wh <- img.wh) {
        o.iconSize = Leaflet.point(wh.width, y = wh.height)
      }
      for (xy <- img.xy) {
        o.iconAnchor = Leaflet.point(xy.x, y = xy.y)
      }
    }
    o
  }


  /** Сборка иконки маркера. */
  def pinMarkerIcon(): Icon = {
    // Начинаем собирать параметры иконки.
    val o = markerIconBase(MarkerIcon)

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
        o.shadowSize = Leaflet.point(wh.width, y = wh.height)
      }
      for (xy <- img.xy) {
        o.shadowAnchor = Leaflet.point(xy.x, y = xy.y)
      }
    }

    // Вернуть итоговый результат.
    Leaflet.icon(o)
  }


  def pendingIcon(url: String, sidePx: Int = LkPreLoader.WIDTH_PX): Icon = {
    val io = IconOptions.empty
    io.iconUrl = url
    io.iconSize = Leaflet.point(x = sidePx, y = sidePx)
    val center = sidePx/2
    io.iconAnchor = Leaflet.point(x = center, y = center)
    Leaflet.icon(io)
  }

}
