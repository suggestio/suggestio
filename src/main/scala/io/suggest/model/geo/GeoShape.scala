package io.suggest.model.geo

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.EsModel
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.menum.EnumJsonReadsValT
import io.suggest.util.MacroLogsDyn
import org.elasticsearch.common.geo.builders.ShapeBuilder
import play.api.data.validation.ValidationError
import play.api.libs.json._
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.08.14 9:43
 * Description: GeoShape - система задания 'ов, которые могут быть различными геометрическими фиругами.
 * В функциях модуля избегаем использования jts из-за возможных проблем с XYZ-координатами в будущем.
 */

object GeoShape extends MacroLogsDyn {

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
          case other =>
            LOGGER.error("deserialize(): Unsupported geo shape type: " + other + "\n data was: " + jmap)
            None
        }
  }

  val TYPE_FORMAT = (__ \ TYPE_ESFN).format[GsType]

  val READS = Reads[GeoShape] {
    case o: JsObject =>
      o.validate(TYPE_FORMAT)
        .flatMap { gsType =>
          o.validate( gsType.companion.DATA_FORMAT )
        }

    case other =>
      JsError( ValidationError("expected.jsobject", other) )
  }

  val WRITES = Writes[GeoShape] { gs =>
    gs.toPlayJson()
  }

  implicit val FORMAT = Format(READS, WRITES)

}


import GeoShape._


/** Базовый трейт для реализаций geoshape. */
trait GeoShape {

  /** Используемый тип фигуры. */
  def shapeType: GsType

  /** Отрендерить json для сохранения внутри _source. */
  def toPlayJson(geoJsonCompatible: Boolean = false): JsObject = {
    val typeName: String = if (geoJsonCompatible) {
      shapeType.geoJsonName.get
    } else {
      shapeType.esName
    }
    val acc = TYPE_ESFN -> JsString(typeName) ::
      _toPlayJsonInternal(geoJsonCompatible)
    JsObject(acc)
  }

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  def _toPlayJsonInternal(geoJsonCompatible: Boolean): FieldsJsonAcc

  def firstPoint: GeoPoint
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
object GsTypes extends Enumeration with EnumMaybeWithName with EnumJsonReadsValT {

  protected[this] abstract sealed class Val(val esName: String)
    extends super.Val(esName)
  {
    /** Имя в рамках спецификации GeoJSON. */
    def geoJsonName: Option[String] = None

    def isGeoJsonCompatible: Boolean = geoJsonName.isDefined

    def companion: GsStatic
  }

  override type T = Val

  val point               : T = new Val("point") {
    override def geoJsonName  = Some("Point")
    override def companion    = PointGs
  }

  val polygon             : T = new Val("polygon") {
    override def geoJsonName  = Some("Polygon")
    override def companion    = PolygonGs
  }

  val circle              : T = new Val("circle") {
    override def companion    = CircleGs
  }

  val linestring          : T = new Val("linestring") {
    override def geoJsonName  = Some("LineString")
    override def companion    = LineStringGs
  }

  val multipoint          : T = new Val("multipoint") {
    override def geoJsonName  = Some("MultiPoint")
    override def companion    = MultiPoingGs
  }

  val multilinestring     : T = new Val("multilinestring") {
    override def geoJsonName  = Some("MultiLineString")
    override def companion    = MultiLineStringGs
  }

  val multipolygon        : T = new Val("multipolygon") {
    override def geoJsonName  = Some("MultiPolygon")
    override def companion    = MultiPolygonGs
  }

  val geometrycollection  : T = new Val("geometrycollection") {
    override def geoJsonName  = Some("GeometryCollection")
    override def companion    = GeometryCollectionGs
  }

  //val envelope            : T = new Val("envelope")

  override def maybeWithName(n: String): Option[T] = {
    values
      .find { v =>
        val _v = value2val(v)
        _v.esName == n || _v.geoJsonName.contains(n)
      }
      .asInstanceOf[Option[T]]
  }

}


trait GsStatic {

  type Shape_t <: GeoShape

  def DATA_FORMAT: Reads[Shape_t]

}
