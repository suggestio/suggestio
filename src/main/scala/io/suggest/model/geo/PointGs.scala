package io.suggest.model.geo

import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import java.{util => ju}
import GeoShape._
import play.api.libs.json.JsValue

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 12:20
 * Description: Sio-представление точки в двумерном пространстве WGS84.
 */

object PointGs {

  def deserialize(jmap: ju.Map[_,_]): Option[PointGs] = {
    Option(jmap.get(COORDS_ESFN))
      .flatMap { GeoPoint.deserializeOpt }
      .map { PointGs.apply }
  }

}


case class PointGs(coord: GeoPoint) extends GeoShapeQuerable {

  override def shapeType = GsTypes.point

  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    List(COORDS_ESFN -> coord.toPlayGeoJson)
  }

  override def toEsShapeBuilder = {
    ShapeBuilder.newPoint(coord.lon, coord.lat)
  }

}


