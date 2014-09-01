package io.suggest.model.geo

import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.{MultiPolygonBuilder, ShapeBuilder}
import play.api.libs.json.JsArray
import GeoShape.COORDS_ESFN
import java.{util => ju, lang => jl}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 18:58
 * Description: Мультиполигон - это список полигонов.
 */
object MultiPolygonGs {

  def deserialize(jmap: ju.Map[_,_]): Option[MultiPolygonGs] = {
    Option(jmap get COORDS_ESFN)
      .map { fromCoordPolys }
  }

  def fromCoordPolys(coords: Any): MultiPolygonGs = {
    coords match {
      case l: TraversableOnce[_] =>
        MultiPolygonGs(
          polygons = l.toIterator.map(PolygonGs.fromCoordLines).toSeq
        )

      case l: jl.Iterable[_] =>
        fromCoordPolys(l.iterator() : TraversableOnce[_])
    }
  }

}


case class MultiPolygonGs(polygons: Seq[PolygonGs]) extends GeoShapeQuerable {

  override def shapeType = GsTypes.multipolygon

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  override def _toPlayJsonInternal: FieldsJsonAcc = {
    val coords = JsArray(polygons.map { _._toPlayJsonCoords })
    List(COORDS_ESFN -> coords)
  }

  /** Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]*/
  override def toEsShapeBuilder: MultiPolygonBuilder = {
    polygons.foldLeft(ShapeBuilder.newMultiPolygon) {
      (mpb, poly) =>
        val polyBuilder = mpb.polygon()
        poly.renderToEsPolyBuilder(polyBuilder)
        polyBuilder.close()
    }
  }

}

