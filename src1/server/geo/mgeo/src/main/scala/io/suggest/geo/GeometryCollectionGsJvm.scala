package io.suggest.geo

import io.suggest.util.JacksonParsing.FieldsJsonAcc
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{Geometry, GeometryCollection, LngLat}
import GeoShapeJvm.GEO_SHAPE_FORMAT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.09.14 18:17
 * Description: Набор геометрических фигур.
 */
object GeometryCollectionGsJvm extends GsStaticJvm {

  val GEOMETRIES_ESFN = "geometries"

  override type Shape_t = GeometryCollectionGs

  override def DATA_FORMAT: Format[Shape_t] = {
    (__ \ GEOMETRIES_ESFN).format[Seq[IGeoShape]]
      .inmap[Shape_t](GeometryCollectionGs.apply, _.geoms)
  }

  /** Конвертация в play.extras.geojson.Geomenty.
    * Circle конвертится в точку!
    * ES envelope -- пока не поддерживается, но можно представить прямоугольным полигоном.
    *
    * @param gs Шейп.
    * @return Геометрия play-geojson.
    */
  override def toPlayGeoJsonGeom(gs: Shape_t): Geometry[LngLat] = {
    GeometryCollection(
      gs.geoms
        .iterator
        .map( GeoShapeJvm.toPlayGeoJsonGeom )
        .toStream
    )
  }

  override protected[this] def _toPlayJsonInternal(gs: Shape_t, geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val geomsJson = gs.geoms.map { subGs =>
      GeoShapeJvm.toPlayJson( subGs, geoJsonCompatible )
    }
    List(
      GEOMETRIES_ESFN -> JsArray(geomsJson)
    )
  }

}
