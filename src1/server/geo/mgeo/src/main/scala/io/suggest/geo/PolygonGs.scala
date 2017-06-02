package io.suggest.geo

import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.geo.GeoShape.COORDS_ESFN
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.{BasePolygonBuilder, ShapeBuilder}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{LngLat, Polygon}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 13:20
 * Description: Sio-класс для полигона.
 */

object PolygonGs extends GsStaticJvm {

  override type Shape_t = PolygonGs

  override def DATA_FORMAT: Format[PolygonGs] = {
    (__ \ COORDS_ESFN).format[List[Seq[MGeoPoint]]]
      .inmap [PolygonGs] (
        apply,
        _.toMpGss
      )
  }

  def apply(lsgss: List[Seq[MGeoPoint]]): PolygonGs = {
    PolygonGs(
      outer = LineStringGs( lsgss.head ),
      holes = lsgss.tail.map(LineStringGs.apply)
    )
  }

}


/** Полигон с необязательными дырками в двумерном пространстве. */
case class PolygonGs(outer: LineStringGs, holes: List[LineStringGs] = Nil) extends GeoShapeQuerable {
  override def shapeType = GsTypes.Polygon

  override def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc = {
    List(COORDS_ESFN -> _toPlayJsonCoords)
  }

  def _toPlayJsonCoords: JsArray = {
    val coords = outer :: holes
    val playJson = coords.map { line => LineStringGs.coords2playJson(line.coords) }
    JsArray(playJson)
  }

  override def toEsShapeBuilder = {
    val poly = ShapeBuilder.newPolygon()
    // Рисуем оболочку
    renderToEsPolyBuilder(poly)
  }

  def renderToEsPolyBuilder[T <: BasePolygonBuilder[T]](poly: BasePolygonBuilder[T]): BasePolygonBuilder[T] = {
    outer.coords foreach { outerGp =>
      poly.point(outerGp.lon, outerGp.lat)
    }
    poly.close()
    // Рисуем дырки
    holes.foreach { hole =>
      val holeRing = poly.hole()
      hole.renderToShape(holeRing)
    }
    poly
  }

  override def firstPoint = outer.firstPoint

  def toMpGss = (outer :: holes).map(_.coords)

  override def toPlayGeoJsonGeom: Polygon[LngLat] = {
    Polygon(
      coordinates = outer.toPlayGeoJsonGeom.coordinates :: holes.map(_.toPlayGeoJsonGeom.coordinates)
    )
  }

}
