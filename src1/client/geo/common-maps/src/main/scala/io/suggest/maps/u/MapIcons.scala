package io.suggest.maps.u

import io.suggest.common.spa.SpaConst.LkPreLoader
import io.suggest.maps.vm.RadiusMarkerIcon
import io.suggest.maps.vm.img.{IconVmStaticT, MarkerIcon, MarkerIconRetina, MarkerIconShadow}
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.common.geo.json.{BooGjFeature, GjGeometry, GjTypes}
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.marker.{Marker, MarkerOptions}
import io.suggest.sjs.leaflet.marker.icon.{Icon, IconOptions}
import io.suggest.maps.m.MarkerNodeId._
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.sjs.leaflet.geojson.GeoJson

import scala.scalajs.js.JSConverters._
import scala.scalajs.js

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


  /** Сборка данных для иконки маркера радиуса круга. */
  def radiusMarkerIcon(): Icon = {
    val o = MapIcons.markerIconBase(RadiusMarkerIcon)
    Leaflet.icon(o)
  }


  /** Скомпилировать GeoJSON-маркеров в маркеры для кластеризации.
    * @param features Исходный набор точек с сервера.
    */
  def geoJsonToClusterMarkers(features: TraversableOnce[BooGjFeature[MAdvGeoMapNodeProps]]): js.Array[Marker] = {
    val iter2 = for {
      // Перебираем все переданные фичи GeoJSON.
      feature <- features

      // Тут интересуют только точки.
      if (feature.geometry.`type` == GjTypes.Geom.POINT) && feature.props.circleRadiusM.isEmpty

    } yield {

      // Собираем параметры отображения маркера.
      val nodeId = feature.props.nodeId
      val options = new MarkerOptions {
        override val draggable = false
        override val clickable = true //nodeIdOpt.isDefined
        // Иконка обязательна, иначе отображать будет нечего. Собрать иконку из присланных сервером данных.
        override val icon = js.defined {
          feature.props.icon.fold( MapIcons.pinMarkerIcon() ) { iconInfo =>
            val o = IconOptions.empty
            o.iconUrl = iconInfo.url
            // Описываем размеры иконки по данным сервера.
            o.iconSize = MapsUtil.size2d2LPoint( iconInfo.wh )
            // Для иконки -- якорь прямо в середине.
            o.iconAnchor = MapsUtil.size2d2LPoint( iconInfo.wh / 2 )
            Leaflet.icon(o)
          }
        }
        override val title = JsOptionUtil.opt2undef( feature.props.hint )
      }

      val m = Leaflet.marker(
        latLng  = GeoJson.coordsToLatLng(
          GjGeometry.firstPoint( feature.geometry )
        ),
        options = options
      )

      // monkey-patching id узла внутрь маркера.
      m.nodeId = nodeId

      m
    }

    iter2.toJSArray
  }

}
