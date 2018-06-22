package io.suggest.maps.u

import diode.data.Pot
import io.suggest.common.spa.SpaConst.LkPreLoaderConst
import io.suggest.i18n.MsgCodes
import io.suggest.maps.vm.RadiusMarkerIcon
import io.suggest.maps.vm.img.{IconVmStaticT, MarkerIcon, MarkerIconRetina, MarkerIconShadow}
import io.suggest.msg.{Messages, WarnMsgs}
import io.suggest.react.ReactCommonUtil
import io.suggest.sjs.common.vm.spa.LkPreLoader
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.marker.icon.{Icon, IconOptions}
import io.suggest.sjs.leaflet.marker.{Marker, MarkerOptions}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.common.log.Log
import japgolly.scalajs.react.vdom.VdomElement

import scala.scalajs.js
// Не удалять: нужно для MarkerR()...
import japgolly.scalajs.react.vdom.Implicits._
import react.leaflet.marker.{MarkerPropsR, MarkerR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.17 15:59
  * Description: Иконки для маркеров на карте.
  */
object MapIcons extends Log {

  /** Пошаренный mutable JSON с извлечёнными данными pin-маркера. */
  private lazy val PIN_MARKER_ICON_BASE = markerIconBase( MarkerIcon )

  def mkDraggableMarker(latLng: LatLng, icon1: Icon): Marker = {
    val options = new MarkerOptions {
      override val icon       = icon1
      override val draggable  = true
    }
    Leaflet.marker(latLng, options)
  }


  def markerIconBase(iconModel: IconVmStaticT, o: IconOptions = IconOptions.empty): IconOptions = {
    // Собираем обычную иконку маркера.
    val iconFindRes = iconModel.find()

    // Leaflet начал сыпать ошибками после ~1.3, если поле .iconUrl пустовато. Нужно отследить, откуда приходят ошибочные данные.
    if (iconFindRes.isEmpty)
      LOG.warn( WarnMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = iconModel )

    for (img <- iconFindRes) {
      if (img.srcOpt.isEmpty)
        LOG.warn( WarnMsgs.IMG_URL_EXPECTED, msg = (img, iconModel) )

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
    //markerIconBase(MarkerIcon)
    val o = js.Object.create(PIN_MARKER_ICON_BASE).asInstanceOf[IconOptions]

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


  def preloaderLMarkerPot(pot: Pot[_], latLng: LatLng): VdomElement = {
    if (pot.isPending) {
      preloaderLMarker( latLng )
    } else {
      ReactCommonUtil.VdomNullElement
    }
  }

  /** Рендер крутилки прямо на карте.
    * @param latLng L-координата маркера-крутилки.
    * @return VdomElement.
    *         Если LkPreLoader.PRELOADER_IMG_URL не инициализирован, то будет null.
    */
  def preloaderLMarker( latLng: LatLng ): VdomElement = {
    LkPreLoader.PRELOADER_IMG_URL.whenDefinedEl { iconUrl =>
      val icon1 = MapIcons.pendingIcon(iconUrl, 16)
      MarkerR(
        new MarkerPropsR {
          override val position  = latLng
          override val draggable = false
          override val clickable = false
          override val icon      = icon1
          override val title     = Messages( MsgCodes.`Please.wait` )
        }
      ): VdomElement
    }
  }

}
