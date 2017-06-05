package io.suggest.geo

import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.geo.GeoShapeJvm.COORDS_ESFN
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

object PolygonGs extends GsStaticJvmQuerable {

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

  override def toPlayGeoJsonGeom(pgs: Shape_t): Polygon[LngLat] = {
    Polygon(
      coordinates = {
        pgs.outerWithHoles
          .iterator
          .map { lsgs =>
            LineStringGsJvm.toPlayGeoJsonGeom(lsgs).coordinates
          }
          .toStream
      }
    )
  }

  override protected[this] def _toPlayJsonInternal(gs: Shape_t, geoJsonCompatible: Boolean): FieldsJsonAcc = {
    val json = PolygonGs._toPlayJsonCoords( gs )
    val row = COORDS_ESFN -> json
    row :: Nil
  }

  protected[geo] def _toPlayJsonCoords(gs: Shape_t): JsArray = {
    val coords = gs.outerWithHoles
    val playJson = coords.map { line => LineStringGsJvm.coords2playJson(line.coords) }
    JsArray(playJson)
  }

  protected[geo] def _renderToEsPolyBuilder[T <: BasePolygonBuilder[T]](gs: Shape_t, poly: BasePolygonBuilder[T]): BasePolygonBuilder[T] = {
    for (outerGp <- gs.outer.coords) {
      poly.point(outerGp.lon, outerGp.lat)
    }
    poly.close()
    // Рисуем дырки
    for (hole <- gs.holes) {
      val holeRing = poly.hole()
      LineStringGsJvm.renderToShape(hole, holeRing)
    }
    poly
  }

  override def toEsShapeBuilder(gs: Shape_t) = {
    val poly = ShapeBuilder.newPolygon()
    // Рисуем оболочку
    PolygonGs._renderToEsPolyBuilder(gs, poly)
  }

}


/** Полигон с необязательными дырками в двумерном пространстве. */
case class PolygonGs(
                      outer : LineStringGs,
                      holes : List[LineStringGs] = Nil
                    )
  extends IGeoShapeQuerable
{

  override def shapeType = GsTypes.Polygon

  override def firstPoint = outer.firstPoint

  def toMpGss = outerWithHoles.map(_.coords)

  def outerWithHoles = outer :: holes

}
