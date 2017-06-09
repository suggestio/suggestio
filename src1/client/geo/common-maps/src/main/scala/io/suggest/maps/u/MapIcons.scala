package io.suggest.maps.u

import diode.data.Pot
import diode.react.ReactPot._
import io.suggest.common.spa.SpaConst.LkPreLoaderConst
import io.suggest.i18n.MsgCodes
import io.suggest.maps.vm.RadiusMarkerIcon
import io.suggest.maps.vm.img.{IconVmStaticT, MarkerIcon, MarkerIconRetina, MarkerIconShadow}
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.vm.spa.LkPreLoader
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.marker.icon.{Icon, IconOptions}
import io.suggest.sjs.leaflet.marker.{Marker, MarkerOptions}
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import japgolly.scalajs.react.{ReactElement, ReactNode}
import react.leaflet.marker.{MarkerPropsR, MarkerR}

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


  def pendingIcon(url: String, sidePx: Int = LkPreLoaderConst.WIDTH_PX): Icon = {
    val io = IconOptions.empty
    io.iconUrl = url
    io.iconSize = Leaflet.point(x = sidePx, y = sidePx)
    val center = sidePx/2
    io.iconAnchor = Leaflet.point(x = center, y = center)
    Leaflet.icon(io)
  }


  /** Сборка данных для иконки маркера радиуса круга. */
  def radiusMarkerIcon(): Icon = {
    val o = MapIcons.markerIconBase(RadiusMarkerIcon)
    Leaflet.icon(o)
  }


  def preloaderLMarkerPot(pot: Pot[_], latLng: LatLng): ReactNode = {
    pot.renderPending { _: Int =>
      preloaderLMarker( latLng )
    }
  }
  /** Рендер крутилки прямо на карте.
    * @param latLng L-координата маркера-крутилки.
    * @return ReactElement.
    *         Если LkPreLoader.PRELOADER_IMG_URL не инициализирован, то будет null.
    */
  def preloaderLMarker( latLng: LatLng ): ReactElement = {
    for (iconUrl <- LkPreLoader.PRELOADER_IMG_URL) yield {
      val icon1 = MapIcons.pendingIcon(iconUrl, 16)
      MarkerR(
        new MarkerPropsR {
          override val position  = latLng
          override val draggable = false
          override val clickable = false
          override val icon      = icon1
          override val title     = Messages( MsgCodes.`Please.wait` )
        }
      )()
    }
  }

}
