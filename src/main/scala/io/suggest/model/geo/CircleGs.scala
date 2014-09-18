package io.suggest.model.geo

import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import play.api.libs.json._
import java.{util => ju}
import GeoShape.COORDS_ESFN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 12:29
 * Description: Круг в двумерном пространстве.
 */
object CircleGs {

  val RADIUS_ESFN = "radius"

  def deserialize(jmap: ju.Map[_,_]): Option[CircleGs] = {
    Option(jmap get COORDS_ESFN).flatMap(GeoPoint.deserializeOpt).flatMap { gp =>
      Option(jmap get RADIUS_ESFN).map(Distance.parseDistance).map { dst =>
        CircleGs(center = gp, radius = dst)
      }
    }
  }

}


import CircleGs._


case class CircleGs(center: GeoPoint, radius: Distance) extends GeoShapeQuerable {
  override def shapeType = GsTypes.circle


  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    List(
      COORDS_ESFN  -> center.toPlayGeoJson,
      RADIUS_ESFN  -> JsString(radius.toString)
    )
  }

  override def toEsShapeBuilder = {
    ShapeBuilder.newCircleBuilder()
      .center(center.lon, center.lat)
      .radius(radius.distance, radius.units)
  }

  override def firstPoint = center
}

