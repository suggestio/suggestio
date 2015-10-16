package io.suggest.model.geo

import GeoShape.COORDS_ESFN
import io.suggest.model.es.EsModelUtil
import EsModelUtil.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.{MultiPointBuilder, PointCollection, ShapeBuilder}
import java.{util => ju}

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 17:50
 * Description: Аналог линии, но не линия, а просто множество точек.
 */
object MultiPoingGs extends MultiPointShapeStatic {
  override type Shape_t = MultiPoingGs
}

case class MultiPoingGs(coords: Seq[GeoPoint]) extends MultiPointShape {

  override def shapeType = GsTypes.multipoint

  override type Shape_t = MultiPointBuilder

  override protected def shapeBuilder: Shape_t = {
    ShapeBuilder.newMultiPoint()
  }
}



/** Общий static-код моделей, которые описываются массивом точек. */
trait MultiPointShapeStatic extends GsStatic  {

  override type Shape_t <: MultiPointShape

  def apply(coords: Seq[GeoPoint]): Shape_t

  def deserialize(jmap: ju.Map[_,_]): Option[Shape_t] = {
    Option(jmap get COORDS_ESFN)
      .map { rawCoords => apply( LineStringGs.parseCoords(rawCoords) ) }
  }

  override def DATA_FORMAT: Format[Shape_t] = {
    (__ \ COORDS_ESFN).format[Seq[GeoPoint]]
      .inmap[Shape_t](apply, _.coords)
  }

}


/** Общий код linestring и multipoint здеся. */
trait MultiPointShape extends GeoShapeQuerable {

  def coords: Seq[GeoPoint]

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
