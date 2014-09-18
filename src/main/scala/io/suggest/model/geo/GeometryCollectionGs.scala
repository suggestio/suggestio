package io.suggest.model.geo

import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json._
import java.{util => ju, lang => jl}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.09.14 18:17
 * Description: Набор геометрических фигур.
 */
object GeometryCollectionGs {

  val GEOMETRIES_ESFN = "geometries"

  def deserialize(jmap: ju.Map[_,_]): Option[GeometryCollectionGs] = {
    Option(jmap get GEOMETRIES_ESFN) map { geomsRaw =>
      GeometryCollectionGs(
        geoms = deserializeGeoms( geomsRaw )
      )
    }
  }

  val deserializeGeoms: PartialFunction[Any, Seq[GeoShape]] = {
    case geomsRaw: TraversableOnce[_] =>
      geomsRaw
        .map(GeoShape.deserialize)
        .flatMap(_.toList)
        .toList
    case geomsRaw: jl.Iterable[_] =>
      deserializeGeoms( geomsRaw.iterator : TraversableOnce[_] )
  }

}


import io.suggest.model.geo.GeometryCollectionGs._


case class GeometryCollectionGs(geoms: Seq[GeoShape]) extends GeoShape {

  override def shapeType = GsTypes.geometrycollection

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val geomsJson = geoms.map { _.toPlayJson(geoJsonCompatible) }
    List(
      GEOMETRIES_ESFN -> JsArray(geomsJson)
    )
  }

  override def firstPoint = geoms.head.firstPoint
}
