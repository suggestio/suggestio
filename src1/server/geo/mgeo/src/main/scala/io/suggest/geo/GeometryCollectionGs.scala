package io.suggest.geo

import io.suggest.util.JacksonParsing.FieldsJsonAcc
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{Geometry, GeometryCollection, LngLat}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.09.14 18:17
 * Description: Набор геометрических фигур.
 */
object GeometryCollectionGs extends GsStaticJvm {

  val GEOMETRIES_ESFN = "geometries"

  override type Shape_t = GeometryCollectionGs

  override def DATA_FORMAT: Format[Shape_t] = {
    (__ \ GEOMETRIES_ESFN).format[Seq[GeoShape]]
      .inmap[Shape_t](apply, _.geoms)
  }

  /** Конвертация в play.extras.geojson.Geomenty.
    * Circle конвертится в точку!
    * ES envelope -- пока не поддерживается, но можно представить прямоугольным полигоном.
    *
    * @param gs Шейп.
    * @return Геометрия play-geojson.
    */
  override def toPlayGeoJsonGeom(gs: GeometryCollectionGs): Geometry[LngLat] = {
    GeometryCollection(
      gs.geoms
        .iterator
        .map( GeoShape.toPlayGeoJsonGeom )
        .toStream
    )
  }

}


import io.suggest.geo.GeometryCollectionGs._


case class GeometryCollectionGs(geoms: Seq[GeoShape]) extends GeoShape {

  override def shapeType = GsTypes.GeometryCollection

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val geomsJson = geoms.map { _.toPlayJson(geoJsonCompatible) }
    List(
      GEOMETRIES_ESFN -> JsArray(geomsJson)
    )
  }

  override def firstPoint = geoms.head.firstPoint

}
