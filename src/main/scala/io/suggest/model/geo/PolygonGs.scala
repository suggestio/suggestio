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
          outer = allCoords.headOption.fold(Seq.empty[GeoPoint])(LineStringGs.parseCoords),
          holes = allCoords.toList.tail.map(LineStringGs.parseCoords)
        )

      case allCoords: jl.Iterable[_] =>
        val allCoordsSeq: Traversable[_] = allCoords
        fromCoordLines(allCoordsSeq)
    }
  }

}


/** Полигон с необязательными дырками в двумерном пространстве. */
case class PolygonGs(outer: Seq[GeoPoint], holes: List[Seq[GeoPoint]] = Nil) extends GeoShape {
  override def shapeType = GsTypes.polygon

  override def _toPlayJsonInternal: FieldsJsonAcc = {
    List(COORDS_ESFN -> _toPlayJsonCoords)
  }

  def _toPlayJsonCoords: JsArray = {
    val coords = outer :: holes
    val playJson = coords.map { LineStringGs.coords2playJson }
    JsArray(playJson)
  }

  override def toEsShapeBuilder = {
    val poly = ShapeBuilder.newPolygon()
    // Рисуем оболочку
    renderToEsPolyBuilder(poly)
  }

  def renderToEsPolyBuilder[T <: BasePolygonBuilder[T]](poly: BasePolygonBuilder[T]): BasePolygonBuilder[T] = {
     outer foreach { outerGp =>
      poly.point(outerGp.lon, outerGp.lat)
    }
    poly.close()
    // Рисуем дырки
    holes.foreach { hole =>
      val holeRing = poly.hole()
      hole.foreach { holeGp =>
        holeRing.point(holeGp.lon, holeGp.lat)
      }
    }
    poly
  }

}
