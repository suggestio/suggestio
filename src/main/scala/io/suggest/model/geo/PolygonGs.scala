package io.suggest.model.geo

import io.suggest.model.es.EsModelUtil
import EsModelUtil.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.{BasePolygonBuilder, ShapeBuilder}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import GeoShape.COORDS_ESFN
import java.{lang => jl, util => ju}

import play.extras.geojson.{LatLng, Polygon}

import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 13:20
 * Description: Sio-класс для полигона.
 */

object PolygonGs extends GsStatic {

  override type Shape_t = PolygonGs

  def deserialize(jmap: ju.Map[_,_]): Option[PolygonGs] = {
    Option(jmap get COORDS_ESFN)
      .map { fromCoordLines }
  }

  def fromCoordLines(coordLines: Any): PolygonGs = {
    coordLines match {
      case allCoords: Traversable[_] =>
        PolygonGs(
          outer = LineStringGs(
            allCoords
              .headOption
              .fold [Seq[GeoPoint]] (Nil) (LineStringGs.parseCoords)
          ),
          holes = {
            val iter = allCoords.toIterator
            if (iter.nonEmpty) {
              iter.next() // типа вызов .tail()
              iter
                .map { ptsRaw =>
                  LineStringGs(LineStringGs.parseCoords(ptsRaw))
                }
                .toList
            } else {
              Nil
            }
          }
        )

      case allCoords: jl.Iterable[_] =>
        val allCoordsSeq: Traversable[_] = allCoords
        fromCoordLines(allCoordsSeq)
    }
  }

  override def DATA_FORMAT: Format[PolygonGs] = {
    (__ \ COORDS_ESFN).format[List[Seq[GeoPoint]]]
      .inmap [PolygonGs] (
        apply,
        _.toMpGss
      )
  }

  def apply(lsgss: List[Seq[GeoPoint]]): PolygonGs = {
    PolygonGs(
      outer = LineStringGs( lsgss.head ),
      holes = lsgss.tail.map(LineStringGs.apply)
    )
  }

}


/** Полигон с необязательными дырками в двумерном пространстве. */
case class PolygonGs(outer: LineStringGs, holes: List[LineStringGs] = Nil) extends GeoShapeQuerable {
  override def shapeType = GsTypes.polygon

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

  override def toPlayGeoJsonGeom: Polygon[LatLng] = {
    Polygon(
      coordinates = outer.toPlayGeoJsonGeom.coordinates :: holes.map(_.toPlayGeoJsonGeom.coordinates)
    )
  }

}
