package io.suggest.lk.adv.geo.u

import io.suggest.geo.{MGeoCircle, MGeoPoint}
import io.suggest.lk.adv.geo.m.MMapGjFeature
import io.suggest.maps.c.LeafletPinMarker
import io.suggest.sjs.common.geo.json.GjTypes
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.marker.icon.{Icon, IconOptions}
import io.suggest.sjs.leaflet.marker.{Marker, MarkerOptions}
import io.suggest.lk.adv.geo.m.MarkerNodeId._
import io.suggest.lk.adv.geo.vm.rad.RadiusMarkerIcon
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.common.model.loc.MGeoPointJs
import io.suggest.sjs.leaflet.map.LatLng

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.16 14:57
  * Description: Статическая утиль для рендера формы георазмещения.
  */
object LkAdvGeoFormUtil extends LeafletPinMarker {

  /** Конверсия L.LatLng в MGeoPoint. */
  def latLng2geoPoint(ll: LatLng): MGeoPoint = {
    MGeoPoint(
      lat = ll.lat,
      lon = ll.lng
    )
  }

  /** Конверсия MGeoPoint в L.LatLng. */
  def geoPoint2LatLng(gp: MGeoPoint): LatLng = {
    Leaflet.latLng( MGeoPointJs.toLatLngArray(gp) )
  }

  /** Сборка данных для иконки маркера радиуса круга. */
  def radiusMarkerIcon(): Icon = {
    val o = _markerIconBase(RadiusMarkerIcon)
    Leaflet.icon(o)
  }

  /** Посчитать расстояние между двумя точками. */
  def distanceBetween(gp0: MGeoPoint, gp1: MGeoPoint): Double = {
    geoPoint2LatLng(gp0)
      .distanceTo( geoPoint2LatLng(gp1) )
  }


  /**
    * Посчитать дефолтовые координаты маркера радиуса на основе указанного круга.
    * @param geoCircle Текущий круг.
    * @return Гео-точка.
    * @see [[http://gis.stackexchange.com/a/2980]]
    */
  def radiusMarkerLatLng(geoCircle: MGeoCircle): MGeoPoint = {
    // Считаем чисто математичеки координаты маркера радиуса. По дефолту, просто восточнее от центра на расстоянии радиуса.
    val earthRadiusM = 6378137

    // offsets in meters: north = +0; east = +radiusM
    // Coord.offsets in radians:
    //val dLat = 0   // пока тут у нас нет смещения на север. Поэтому просто ноль.
    val D180 = 180
    // Изображаем дробь:
    val dLon = geoCircle.radiusM / (
      earthRadiusM * Math.cos(
        Math.PI * geoCircle.center.lat / D180
      )
    )
    geoCircle.center.withLon(
      // OffsetPosition, decimal degrees
      geoCircle.center.lon + dLon * D180 / Math.PI
    )
  }


  /** Скомпилировать GeoJSON-маркеров в маркеры для кластеризации.
    * @param gjFeatures Исходный набор точек с сервера.
    */
  def geoJsonToClusterMarkers(gjFeatures: TraversableOnce[MMapGjFeature]): js.Array[Marker] = {
    val iter2 = for {
      // Перебираем все переданные фичи GeoJSON.
      gjFeature <- gjFeatures

      // Тут интересуют только точки.
      if gjFeature.underlying.geometry.`type` == GjTypes.Geom.POINT

    } yield {

      // Собираем параметры отображения маркера.
      val nodeIdOpt = gjFeature.nodeId
      val options = new MarkerOptions {
        override val draggable = false
        override val clickable = nodeIdOpt.isDefined
        // Иконка обязательна, иначе отображать будет нечего. Собрать иконку из присланных сервером данных.
        override val icon = gjFeature.icon.fold(_pinMarkerIcon()) { iconInfo =>
          val o = IconOptions.empty
          o.iconUrl = iconInfo.url
          // Описываем размеры иконки по данным сервера.
          o.iconSize = Leaflet.point(
            y = iconInfo.height,
            x = iconInfo.width
          )
          // Для иконки -- якорь прямо в середине.
          o.iconAnchor = Leaflet.point(
            y = iconInfo.height / 2,
            x = iconInfo.width / 2
          )
          Leaflet.icon(o)
        }
        override val title = JsOptionUtil.opt2undef( gjFeature.title )
      }

      val m = Leaflet.marker(
        latLng  = gjFeature.pointLatLng,
        options = options
      )

      // monkey-patching id узла внутрь маркера.
      for (nodeId <- nodeIdOpt) {
        m.nodeId = nodeId
      }

      m
    }

    iter2.toJSArray
  }

}
