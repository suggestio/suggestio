package io.suggest.model.geo

import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.{BasePolygonBuilder, ShapeBuilder}
import play.api.libs.json._
import GeoShape.COORDS_ESFN
import java.{util => ju, lang => jl}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 13:20
 * Description: Sio-класс для полигона.
 */

object PolygonGs {

  def deserialize(jmap: ju.Map[_,_]): Option[PolygonGs] = {
    Option(jmap get COORDS_ESFN)
      .map { fromCoordLines }
  }

  def fromCoordLines(coordLines: Any): PolygonGs = {
    coordLines match {
      case allCoords: Traversable[_] =>
        PolygonGs(
          outer = LineStringGs( allCoords.headOption.fold(Seq.empty[GeoPoint])(LineStringGs.parseCoords) ),
          holes = allCoords.toList.tail.map { ptsRaw => LineStringGs(LineStringGs.parseCoords(ptsRaw)) }
        )

      case allCoords: jl.Iterable[_] =>
        val allCoordsSeq: Traversable[_] = allCoords
        fromCoordLines(allCoordsSeq)
    }
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
}
