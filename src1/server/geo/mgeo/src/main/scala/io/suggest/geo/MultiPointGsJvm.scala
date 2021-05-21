package io.suggest.geo

import org.elasticsearch.common.geo.builders.MultiPointBuilder
import au.id.jazzy.play.geojson.{LngLat, MultiPoint}
import org.locationtech.jts.geom.Coordinate

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

  override def toEsShapeBuilder(gs: MultiPointGs) =
    new MultiPointBuilder( gsCoords2esCoords(gs) )

  def geoPoints2esCoords(points: Seq[MGeoPoint]): ju.List[Coordinate] = {
    import scala.jdk.CollectionConverters._
    points
      .map { GeoPoint.toJtsCoordinate }
      .asJava
  }

}



/** Общий static-код моделей, которые описываются массивом точек. */
trait MultiPointShapeStatic extends GsStaticJvmQuerable {

  override type Shape_t <: MultiPointShape

  /** Сборка immutable-коллекции из инстансов LatLng. */
  def coords2latLngs(coords: IterableOnce[MGeoPoint]): Seq[LngLat] = {
    coords
      .iterator
      .map( GeoPoint.toLngLat )
      .toSeq
  }

  def gsCoords2esCoords(gs: Shape_t): ju.List[Coordinate] = {
    MultiPointGsJvm.geoPoints2esCoords( gs.coords )
  }

}
