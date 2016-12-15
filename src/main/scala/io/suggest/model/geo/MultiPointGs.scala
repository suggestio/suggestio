package io.suggest.model.geo

import GeoShape.COORDS_ESFN
import io.suggest.model.es.EsModelUtil
import EsModelUtil.FieldsJsonAcc
import io.suggest.geo.MGeoPoint
import io.suggest.model.geo.GeoPoint.Implicits._
import org.elasticsearch.common.geo.builders.{MultiPointBuilder, PointCollection, ShapeBuilder}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.extras.geojson.{LatLng, MultiPoint}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 17:50
 * Description: Аналог линии, но не линия, а просто множество точек.
 */
object MultiPointGs extends MultiPointShapeStatic {

  override type Shape_t = MultiPointGs

}

case class MultiPointGs(coords: Seq[MGeoPoint]) extends MultiPointShape {

  override def shapeType = GsTypes.multipoint

  override type Shape_t = MultiPointBuilder

  override protected def shapeBuilder: Shape_t = {
    ShapeBuilder.newMultiPoint()
  }

  override def toPlayGeoJsonGeom: MultiPoint[LatLng] = {
    MultiPoint(
      coordinates = MultiPointGs.coords2latLngs(coords)
    )
  }

}



/** Общий static-код моделей, которые описываются массивом точек. */
trait MultiPointShapeStatic extends GsStatic {

  override type Shape_t <: MultiPointShape

  def apply(coords: Seq[MGeoPoint]): Shape_t

  override def DATA_FORMAT: Format[Shape_t] = {
    (__ \ COORDS_ESFN).format[Seq[MGeoPoint]]
      .inmap[Shape_t](apply, _.coords)
  }

  /** Сборка immutable-коллекции из инстансов LatLng. */
  def coords2latLngs(coords: TraversableOnce[MGeoPoint]): Stream[LatLng] = {
    coords
      .toIterator
      .map( GeoPoint.toLatLng )
      .toStream
  }

}


/** Общий код linestring и multipoint здеся. */
trait MultiPointShape extends GeoShapeQuerable {

  def coords: Seq[MGeoPoint]

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val coordsJson = LineStringGs.coords2playJson(coords)
    List(COORDS_ESFN -> coordsJson)
  }

  type Shape_t <: PointCollection[Shape_t]

  /** Отрендерить в изменяемый LineString ShapeBuilder для построения ES-запросов.
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]
    */
  override def toEsShapeBuilder: Shape_t = {
    renderToShape(shapeBuilder)
  }
  
  def renderToShape[R <: PointCollection[R]](shape: R) = {
    coords.foldLeft(shape) {
      (acc, gp)  =>  acc.point(gp.lon, gp.lat)
    }
  }

  protected def shapeBuilder: Shape_t

  override def shapeType: GsType = GsTypes.multipoint

  override def firstPoint = coords.head

}
