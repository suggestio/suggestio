package io.suggest.model.geo

import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
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
      .map { deserilizeCoords2polygon }
  }

  def deserilizeCoords2polygon(coordLines: Any): PolygonGs = {
    coordLines match {
      case allCoords: Traversable[_] =>
        PolygonGs(
          outer = allCoords.headOption.fold(Seq.empty[GeoPoint])(LineStringGs.parseCoords),
          holes = allCoords.toList.tail.map(LineStringGs.parseCoords)
        )

      case allCoords: jl.Iterable[_] =>
        val allCoordsSeq: Traversable[_] = allCoords
        deserilizeCoords2polygon(allCoordsSeq)
    }
  }

}


/** Полигон с необязательными дырками в двумерном пространстве. */
case class PolygonGs(outer: Seq[GeoPoint], holes: List[Seq[GeoPoint]] = Nil) extends GeoShape {
  override def shapeType = GsTypes.polygon

  override def _toPlayJsonInternal: FieldsJsonAcc = {
    val coords = outer :: holes
    val playJson = coords.map { LineStringGs.coords2playJson }
    List(COORDS_ESFN -> JsArray(playJson))
  }

  override def toEsShapeBuilder = {
    val poly = ShapeBuilder.newPolygon()
    // Рисуем оболочку
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
