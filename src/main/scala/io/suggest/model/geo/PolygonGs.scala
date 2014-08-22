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

  def parseCoords: PartialFunction[Any, Seq[GeoPoint]] = {
    case l: jl.Iterable[_] =>
      l.flatMap { gpRaw => GeoPoint.deserializeOpt(gpRaw) }
        .toSeq
  }

  def deserialize(jmap: ju.Map[_,_]): Option[PolygonGs] = {
    Option(jmap get COORDS_ESFN) map {
      case allCoords: jl.Iterable[_] =>
        val allCoordsSeq: Traversable[_] = allCoords
        PolygonGs(
          outer = allCoords.headOption.fold(Seq.empty[GeoPoint])(parseCoords),
          holes = allCoordsSeq.toList.tail.map(parseCoords)
        )
    }
  }

}


/** Полигон с необязательными дырками в двумерном пространстве. */
case class PolygonGs(outer: Seq[GeoPoint], holes: List[Seq[GeoPoint]] = Nil) extends GeoShape {
  override def shapeType = GsTypes.polygon

  override def _toPlayJsonInternal: FieldsJsonAcc = {
    val coords = outer :: holes
    val playJson = coords.map { contours =>
      val contours2 = contours map { gp =>
        gp.toPlayGeoJson
      }
      JsArray(contours2)
    }
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
