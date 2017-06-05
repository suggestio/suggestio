package io.suggest.geo

import io.suggest.geo.GeoShapeJvm.COORDS_ESFN
import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{Geometry, LngLat}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 12:29
 * Description: Круг в двумерном пространстве.
 */
object CircleGs extends GsStaticJvmQuerable {

  override type Shape_t = CircleGs

  val RADIUS_ESFN = "radius"

  override def DATA_FORMAT: OFormat[CircleGs] = (
    (__ \ COORDS_ESFN).format[MGeoPoint] and
    (__ \ RADIUS_ESFN).format[Distance]
  )(apply, unlift(unapply))

  def apply(geoCircle: MGeoCircle): CircleGs = {
    CircleGs(
      center = geoCircle.center,
      radius = Distance.meters( geoCircle.radiusM )
    )
  }

  /** Вернуть инстанс круга из инстанса гео-шейпа.
    *
    * @param gs Какой-то [[GeoShapeJvm]].
    * @return Опциональный [[CircleGs]].
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
    PointGs.toPlayGeoJsonGeom( circle.center )
  }

  override protected[this] def _toPlayJsonInternal(gs: Shape_t, geoJsonCompatible: Boolean): FieldsJsonAcc = {
    List(
      COORDS_ESFN  -> GeoPoint.toPlayGeoJson( gs.center ),
      RADIUS_ESFN  -> JsString( gs.radius.toString )
    )
  }

  override def toEsShapeBuilder(gs: Shape_t) = {
    ShapeBuilder.newCircleBuilder()
      .center(gs.center.lon, gs.center.lat)
      .radius(gs.radius.distance, gs.radius.units)
  }

}


case class CircleGs(center: MGeoPoint, radius: Distance) extends IGeoShapeQuerable {

  override def shapeType = GsTypes.Circle

  override def firstPoint = center

  override def centerPoint = Some(center)

}

