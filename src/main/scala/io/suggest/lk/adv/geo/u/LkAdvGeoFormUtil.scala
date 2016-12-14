package io.suggest.lk.adv.geo.u

import io.suggest.lk.adv.geo.m.MMapGjFeature
import io.suggest.maps.c.LeafletPinMarker
import io.suggest.sjs.common.geo.json.GjTypes
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.marker.icon.IconOptions
import io.suggest.sjs.leaflet.marker.{Marker, MarkerOptions}
import io.suggest.lk.adv.geo.m.MarkerNodeId._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.16 14:57
  * Description: Статическая утиль для рендера формы георазмещения.
  */
object LkAdvGeoFormUtil extends LeafletPinMarker {


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
      val options = MarkerOptions.empty
      options.draggable = false

      val nodeIdOpt = gjFeature.nodeId
      options.clickable = nodeIdOpt.isDefined

      // Иконка обязательна, иначе отображать будет нечего. Собрать иконку из присланных сервером данных.
      options.icon = gjFeature.icon.fold(_pinMarkerIcon()) { iconInfo =>
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

      for (title <- gjFeature.title)
        options.title = title

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
