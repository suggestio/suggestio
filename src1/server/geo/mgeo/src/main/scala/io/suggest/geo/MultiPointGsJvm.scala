package io.suggest.geo

import au.id.jazzy.play.geojson.{LngLat, MultiPoint}
import org.elasticsearch.geometry.{MultiPoint => EsMultiPoint, Point => EsPoint}
import org.locationtech.spatial4j.shape.Shape

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

  override def toEsShapeBuilder(gs: MultiPointGs) = {
    new EsMultiPoint( gsCoords2esCoords(gs) )
  }

  def geoPoints2esCoords(points: Seq[MGeoPoint]): ju.List[EsPoint] = {
    import scala.jdk.CollectionConverters._
    points
      .map { GeoPoint.toEsPoint }
      .asJava
  }

  /** Convert to spatial4j shape. */
  override def toSpatialShape(gs: MultiPointGs): Shape = {
    val builder = GeoShapeJvm.S4J_CONTEXT.getShapeFactory.multiPoint()

    for (pt <- gs.coords)
      builder.pointXY( pt.lon.doubleValue, pt.lat.doubleValue )

    builder.build()
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

  def gsCoords2esCoords(gs: Shape_t): ju.List[EsPoint] = {
    MultiPointGsJvm.geoPoints2esCoords( gs.coords )
  }

}
