package io.suggest.geo

import com.vividsolutions.jts.geom.Coordinate
import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.geo.GeoShapeJvm.COORDS_ESFN
import io.suggest.primo.IApply1
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilders
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{LngLat, MultiPoint}
import java.{util => ju}

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

  override def toEsShapeBuilder(gs: MultiPointGs) = ShapeBuilders.newMultiPoint( gsCoords2esCoords(gs) )

  override protected[this] def applier = MultiPointGs

  def geoPoints2esCoords(points: Seq[MGeoPoint]): ju.List[Coordinate] = {
    import scala.collection.JavaConversions._
    points
      .map { GeoPoint.toJtsCoordinate }
  }

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

  def gsCoords2esCoords(gs: Shape_t): ju.List[Coordinate] = {
    MultiPointGsJvm.geoPoints2esCoords( gs.coords )
  }

}
