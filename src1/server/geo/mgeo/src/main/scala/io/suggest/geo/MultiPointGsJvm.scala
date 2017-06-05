package io.suggest.geo

import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.geo.GeoShapeJvm.COORDS_ESFN
import io.suggest.primo.IApply1
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.{MultiPointBuilder, PointCollection, ShapeBuilder}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{LngLat, MultiPoint}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 17:50
 * Description: Аналог линии, но не линия, а просто множество точек.
 */
object MultiPointGsJvm extends MultiPointShapeStatic {

  override type Shape_t = MultiPointGs

  def toPlayGeoJsonGeom(gs: Shape_t): MultiPoint[LngLat] = {
    MultiPoint(
      coordinates = MultiPointGsJvm.coords2latLngs( gs.coords )
    )
  }

  override type ShapeBuilder_t = MultiPointBuilder

  override protected def shapeBuilder: ShapeBuilder_t = {
    ShapeBuilder.newMultiPoint()
  }

  override protected[this] def applier = MultiPointGs
}



/** Общий static-код моделей, которые описываются массивом точек. */
trait MultiPointShapeStatic extends GsStaticJvmQuerable {

  override type Shape_t <: MultiPointShape

  protected[this] def applier: IApply1 { type ApplyArg_t = Seq[MGeoPoint]; type T = Shape_t }

  override def DATA_FORMAT: Format[Shape_t] = {
    (__ \ COORDS_ESFN).format[Seq[MGeoPoint]]
      .inmap[Shape_t](applier.apply, _.coords)
  }

  /** Сборка immutable-коллекции из инстансов LatLng. */
  def coords2latLngs(coords: TraversableOnce[MGeoPoint]): Stream[LngLat] = {
    coords
      .toIterator
      .map( GeoPoint.toLngLat )
      .toStream
  }

  override protected[this] def _toPlayJsonInternal(gs: Shape_t, geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val coordsJson = LineStringGsJvm.coords2playJson( gs.coords )
    List(COORDS_ESFN -> coordsJson)
  }

  def renderToShape[R <: PointCollection[R]](gs: Shape_t, shape: R) = {
    gs.coords.foldLeft(shape) {
      (acc, gp)  =>  acc.point(gp.lon, gp.lat)
    }
  }

  type ShapeBuilder_t <: PointCollection[ShapeBuilder_t]

  protected def shapeBuilder: ShapeBuilder_t

  /** Отрендерить в изменяемый LineString ShapeBuilder для построения ES-запросов.
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]
    */
  override def toEsShapeBuilder(gs: Shape_t): ShapeBuilder_t = {
    renderToShape(gs, shapeBuilder)
  }

}