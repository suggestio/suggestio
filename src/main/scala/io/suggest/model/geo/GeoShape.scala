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
          case GsTypes.point              => PointGs.deserialize(jmap)
          case GsTypes.circle             => CircleGs.deserialize(jmap)
          case GsTypes.polygon            => PolygonGs.deserialize(jmap)
          case GsTypes.linestring         => LineStringGs.deserialize(jmap)
          case GsTypes.multipoint         => MultiPoingGs.deserialize(jmap)
          case GsTypes.multilinestring    => MultiLineStringGs.deserialize(jmap)
          case GsTypes.multipolygon       => MultiPolygonGs.deserialize(jmap)
          case GsTypes.geometrycollection => GeometryCollectionGs.deserialize(jmap)
        }
  }

}


import GeoShape._


/** Базовый трейт для реализаций geoshape. */
trait GeoShape {

  /** Используемый тип фигуры. */
  def shapeType: GsTypes.GsType

  /** Отрендерить json для сохранения внутри _source. */
  def toPlayJson(geoJsonCompatible: Boolean = false): JsObject = {
    val typeName: String = if (geoJsonCompatible) {
      shapeType.geoJsonName.get
    } else {
      shapeType.esName
    }
    val acc = TYPE_ESFN -> JsString(typeName) :: _toPlayJsonInternal(geoJsonCompatible)
    JsObject(acc)
  }

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc

}


/** Если элемент можно запрашивать в geo-shape search/filter, то нужен билдер для Shape'а. */
trait GeoShapeQuerable extends GeoShape {

  /**
   * Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]
   */
  def toEsShapeBuilder: ShapeBuilder

}


/** Типы плоских фигур, допустимых для отправки в ES для geo-поиска/geo-фильтрации. */
object GsTypes extends Enumeration {

  protected case class Val(esName: String, geoJsonName: Option[String]) extends super.Val(esName) {
    def isGeoJsonCompatible: Boolean = geoJsonName.isDefined
  }

  type GsType = Val

  val point               = Val("point", Some("Point"))
  val linestring          = Val("linestring", Some("LineString"))
  val polygon             = Val("polygon", Some("Polygon"))
  val multipoint          = Val("multipoint", Some("MultiPoint"))
  val multilinestring     = Val("multilinestring", Some("MultiLineString"))
  val multipolygon        = Val("multipolygon", Some("MultiPolygon"))
  val geometrycollection  = Val("geometrycollection", Some("GeometryCollection"))
  val envelope            = Val("envelope", None)
  val circle              = Val("circle", None)

  implicit def value2val(x: Value): GsType = x.asInstanceOf[GsType]

  def maybeWithName(n: String): Option[GsType] = {
    values
      .find { v =>
        val _v: GsType = v
        _v.esName == n || _v.geoJsonName.exists(_ == n)
      }
      .asInstanceOf[Option[GsType]]
  }

}

