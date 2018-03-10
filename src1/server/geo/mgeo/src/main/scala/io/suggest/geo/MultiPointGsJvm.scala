package io.suggest.geo

import com.vividsolutions.jts.geom.Coordinate
import org.elasticsearch.common.geo.builders.ShapeBuilders
import au.id.jazzy.play.geojson.{LngLat, MultiPoint}
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

  def geoPoints2esCoords(points: Seq[MGeoPoint]): ju.List[Coordinate] = {
    import scala.collection.JavaConverters._
    points
      .map { GeoPoint.toJtsCoordinate }
      .asJava
  }

}



/** Общий static-код моделей, которые описываются массивом точек. */
trait MultiPointShapeStatic extends GsStaticJvmQuerable {

  override type Shape_t <: MultiPointShape

  /** Сборка immutable-коллекции из инстансов LatLng. */
  def coords2latLngs(coords: TraversableOnce[MGeoPoint]): Stream[LngLat] = {
    coords
      .toIterator
      .map( GeoPoint.toLngLat )
      .toStream
  }

  def gsCoords2esCoords(gs: Shape_t): ju.List[Coordinate] = {
    MultiPointGsJvm.geoPoints2esCoords( gs.coords )
  }

}
