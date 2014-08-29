package io.suggest.model.geo

import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.{MultiLineStringBuilder, ShapeBuilder}
import play.api.libs.json._
import GeoShape.COORDS_ESFN
import java.{util => ju, lang => jl}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 18:32
 * Description: Представление multiline-объектов в рамках s.io.
 */

object MultiLineStringGs {

  def deserialize(jmap: ju.Map[_,_]): Option[MultiLineStringGs] = {
    Option(jmap get COORDS_ESFN)
      .map { fromCoordLines }
  }


  def fromCoordLines(coordLines: Any): MultiLineStringGs = {
    coordLines match {
      case allCoordLines: TraversableOnce[_] =>
        MultiLineStringGs(
          coords = allCoordLines.map(LineStringGs.parseCoords).toSeq
        )
      case allCoordLines: jl.Iterable[_] =>
        fromCoordLines( allCoordLines.toIterator )
    }
  }
}


case class MultiLineStringGs(coords: Seq[Seq[GeoPoint]]) extends GeoShape {

  /** Используемый тип фигуры. */
  override def shapeType = GsTypes.multilinestring

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  override def _toPlayJsonInternal: FieldsJsonAcc = {
    val playJson = coords.map { LineStringGs.coords2playJson }
    List(COORDS_ESFN -> JsArray(playJson))
  }

  /** Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]*/
  override def toEsShapeBuilder: MultiLineStringBuilder = {
    // Заливаем линии
    coords.foldLeft(ShapeBuilder.newMultiLinestring) {
      (acc0, coordLine) =>
        // Заливаем все точки в линию
        coordLine.foldLeft(acc0.linestring) {
          (acc1, e)  =>  acc1.point(e.lon, e.lat)
        }.end()
    }
  }

}
