package io.suggest.geo

import au.id.jazzy.play.geojson.{Geometry, LngLat}
import io.suggest.geo.GeoShapeJvm.COORDS_ESFN
import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilders
import org.elasticsearch.common.unit.DistanceUnit
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 12:29
 * Description: Круг в двумерном пространстве.
 */
object CircleGsJvm extends GsStaticJvmQuerable {

  override type Shape_t = CircleGs

  val RADIUS_ESFN = "radius"

  override def DATA_FORMAT: OFormat[CircleGs] = (
    (__ \ COORDS_ESFN).format[MGeoPoint] and
    (__ \ RADIUS_ESFN).format[Distance]
      .inmap[Double]( _.meters, Distance.meters)
  )(CircleGs.apply, unlift(CircleGs.unapply))

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

  override protected[this] def _toPlayJsonInternal(gs: Shape_t, geoJsonCompatible: Boolean): FieldsJsonAcc = {
    List(
      COORDS_ESFN  -> GeoPoint.toPlayGeoJson( gs.center ),
      RADIUS_ESFN  -> JsString( distance(gs).toString )
    )
  }

  def distance(circle: CircleGs) = Distance.meters( circle.radiusM )

  override def toEsShapeBuilder(gs: Shape_t) = {
    ShapeBuilders.newCircleBuilder()
      .center(gs.center.lon, gs.center.lat)
      .radius(gs.radiusM, DistanceUnit.METERS)
  }

}


