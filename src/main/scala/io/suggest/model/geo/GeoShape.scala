package io.suggest.model.geo

import io.suggest.model.EsModel
import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.common.geo.builders.ShapeBuilder
import play.api.libs.json._
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 9:43
 * Description: GeoShape - система задания 'ов, которые могут быть различными геометрическими фиругами.
 * В функциях модуля избегаем использования jts из-за возможных проблем с XYZ-координатами в будущем.
 */

object GeoShape {

  val COORDS_ESFN = "coordinates"
  val TYPE_ESFN   = "type"

  val deserialize: PartialFunction[Any, Option[GeoShape]] = {
    case jmap: ju.Map[_,_] =>
      Option(jmap get TYPE_ESFN)
        .map(EsModel.stringParser)
        .flatMap { GsTypes.maybeWithName }
        .flatMap {
          case GsTypes.point    => PointGs.deserialize(jmap)
          case GsTypes.circle   => CircleGs.deserialize(jmap)
          case GsTypes.polygon  => PolygonGs.deserialize(jmap)
        }
  }
}


import GeoShape._


/** Базовый трейт для реализаций geoshape. */
trait GeoShape {

  /** Используемый тип фигуры. */
  def shapeType: GsTypes.GsType

  /** Отрендерить json для сохранения внутри _source. */
  def toPlayJson: JsObject = {
    val acc = TYPE_ESFN -> JsString(shapeType.toString) :: _toPlayJsonInternal
    JsObject(acc)
  }

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  def _toPlayJsonInternal: FieldsJsonAcc

  /** Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]] */
  def toEsShapeBuilder: ShapeBuilder

}

/** Типы плоских фигур, допустимых для отправки в ES для geo-поиска/geo-фильтрации. */
object GsTypes extends Enumeration {
  type GsType = Value
  val point, linestring, polygon, multipoint, multilinestring, multipolygon, geometrycollection, envelope, circle = Value

  def maybeWithName(n: String): Option[GsType] = {
    try {
      Some(withName(n))
    } catch {
      case ex: Exception => None
    }
  }
}
