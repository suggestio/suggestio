package io.suggest.geo

import au.id.jazzy.play.geojson.{Geometry, LngLat}
import org.elasticsearch.common.geo.builders.ShapeBuilders
import org.elasticsearch.common.unit.DistanceUnit

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 12:29
 * Description: Круг в двумерном пространстве.
 */
object CircleGsJvm extends GsStaticJvmQuerable {

  override type Shape_t = CircleGs

  def apply(geoCircle: CircleGs): CircleGs = {
    CircleGs(
      center  = geoCircle.center,
      radiusM = geoCircle.radiusM
    )
  }

  /** Вернуть инстанс круга из инстанса гео-шейпа.
    *
    * @param gs Какой-то [[GeoShapeJvm]].
    * @return Опциональный [[CircleGsJvm]].
    *         None, если gs -- это НЕ круг, а что-либо другое.
    */
  def maybeFromGs(gs: IGeoShape): Option[CircleGs] = {
    gs match {
      case circle: CircleGs   => Some(circle)
      case _                  => None
    }
  }

  /** Circle представляется точкой, т.к. GeoJSON не поддерживает Circle. */
  override def toPlayGeoJsonGeom(circle: CircleGs): Geometry[LngLat] = {
    PointGsJvm.toPlayGeoJsonGeom( circle.center )
  }

  def distance(circle: CircleGs) = Distance.meters( circle.radiusM )

  override def toEsShapeBuilder(gs: Shape_t) = {
    ShapeBuilders.newCircleBuilder()
      .center(gs.center.lon.doubleValue(), gs.center.lat.doubleValue())
      .radius(gs.radiusM, DistanceUnit.METERS)
  }

}


