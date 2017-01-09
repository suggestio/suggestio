package io.suggest.lk.adv.geo.m

import io.suggest.adv.geo.AdvGeoConstants.AdnNodes._
import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.leaflet.geojson.GeoJson
import io.suggest.sjs.leaflet.map.LatLng

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.11.16 21:49
  * Description: Модель ответа на запрос GeoJSON'а карты lk-adv-geo.
  */
object MMapGjResp {

  def apply(raw: js.Dynamic): MMapGjResp = {
    val s = raw
      .asInstanceOf[ js.Array[GjFeature] ]
    apply(s)
  }

}


/** Класс модели JSON-ответа сервера на запрос GeoJSON'а карты. */
case class MMapGjResp(arr: IndexedSeq[GjFeature]) {

  def featuresIter: Iterator[MMapGjFeature] = {
    arr.iterator
      // TODO Удалить это, когда будет решена проблема с сериализацией в LkAdvGeo. Там в конце списка фич null добавлялся из-за проблем с запятыми при поточной json-сериализации.
      .filter(_ != null)
      .map( MMapGjFeature.apply )
  }

}


case class MMapGjFeature(underlying: GjFeature) {

  val propsOpt = underlying.properties.toOption

  private def prop[T](name: String): Option[T] = {
    propsOpt
      .flatMap( _.get(name) )
      .asInstanceOf[Option[T]]
  }

  def title: Option[String] = prop[String]( HINT_FN )

  def icon: Option[MMapGjIconProp] = {
    for {
      jsObj <- prop [js.Dictionary[js.Any]] (ICON_FN)
    } yield {
      MMapGjIconProp(jsObj)
    }
  }

  /**
    * L.latLng() не умеет правильно декодить geoJSON-массив координат. Но это поправимо...
    * @return LatLng
    *         exception, если геомертрия не является точечной.
    */
  def pointLatLng: LatLng = {
    val lngLat = underlying.geometry
      .coordinates
      .asInstanceOf[js.Array[Double]]
    GeoJson.coordsToLatLng(lngLat)
  }

  /** id узла, относящегося к этому маркеру. */
  def nodeId: Option[String] = prop[String]( NODE_ID_FN )

}


case class MMapGjIconProp(underlying: collection.Map[String, js.Any]) {

  def url     = underlying(Icon.URL_FN).asInstanceOf[String]
  def width   = underlying(Icon.WIDTH_FN).asInstanceOf[Int]
  def height  = underlying(Icon.HEIGHT_FN).asInstanceOf[Int]

}
