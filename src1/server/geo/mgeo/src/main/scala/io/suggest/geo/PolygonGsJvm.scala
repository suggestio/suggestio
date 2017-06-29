package io.suggest.geo

import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.geo.GeoShapeJvm.COORDS_ESFN
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import org.elasticsearch.common.geo.builders._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.extras.geojson.{LngLat, Polygon}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 13:20
 * Description: Sio-класс для полигона.
 */

object PolygonGsJvm extends GsStaticJvmQuerable {

  override type Shape_t = PolygonGs

  override def DATA_FORMAT: Format[Shape_t] = {
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
    val json = PolygonGsJvm._toPlayJsonCoords( gs )
    val row = COORDS_ESFN -> json
    row :: Nil
  }

  protected[geo] def _toPlayJsonCoords(gs: Shape_t): JsArray = {
    val coords = gs.outerWithHoles
    val playJson = coords.map { line => LineStringGsJvm.coords2playJson(line.coords) }
    JsArray(playJson)
  }


  override def toEsShapeBuilder(gs: Shape_t): PolygonBuilder = {
    val inner = new CoordinatesBuilder()
      .coordinates( MultiPointGsJvm.geoPoints2esCoords(gs.outer.coords) )
      .close()

    gs.holes.foldLeft( ShapeBuilders.newPolygon( inner ) ) { (pb, lsGs) =>
      val lsb = LineStringGsJvm.toEsShapeBuilder( lsGs )
      pb.hole( lsb, true )
    }
  }

}
