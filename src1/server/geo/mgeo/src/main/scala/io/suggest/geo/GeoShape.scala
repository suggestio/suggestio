package io.suggest.geo

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsValT
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import io.suggest.util.logs.MacroLogsDyn
import org.elasticsearch.common.geo.builders.ShapeBuilder
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.extras.geojson.{Geometry, LatLng}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.14 9:43
  * Description: GeoShape - система задания'ов, которые могут быть различными геометрическими фиругами.
  * В функциях модуля избегаем использования jts из-за возможных проблем с XYZ-координатами в будущем.
  *
  * Эта модель служит цели
  */

object GeoShape extends MacroLogsDyn {

  /** Распарсить шейп из json-выхлопа. */
  def parse(raw: String): GeoShape = {
    Json.parse(raw).as[GeoShape]
  }

  val COORDS_ESFN = "coordinates"
  val TYPE_ESFN   = "type"

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
    gs.toPlayJson(false)
  }

  implicit val FORMAT = Format(READS, WRITES)

  def WRITES_GJSON_COMPAT: OWrites[GeoShape] = {
    OWrites { v =>
      v.toPlayJson(true)
    }
  }

  def FORMAT_GJSON_COMPAT: OFormat[GeoShape] = {
    OFormat(READS, WRITES_GJSON_COMPAT)
  }

}


import io.suggest.geo.GeoShape._


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

  def firstPoint: MGeoPoint

  /**
    * Центральная точка фигуры.
    * По идее, эта точка всегда существует, но тут Option.
    * None означает, что код поддержки вычисления центральной точки не заимплеменчен.
    */
  def centerPoint: Option[MGeoPoint] = None

  /** Отображаемое для пользователя имя шейпа. */
  def displayTypeName: String = {
    shapeType.geoJsonName
      .getOrElse( getClass.getSimpleName )
  }

  /** Конвертация в play.extras.geojson.Geomenty.
    * Circle конвертится в точку!
    * ES envelope -- пока не поддерживается, но можно представить прямоугольным полигоном.
    */
  def toPlayGeoJsonGeom: Geometry[LatLng]

}


/** Если элемент можно запрашивать в geo-shape search/filter, то нужен билдер для Shape'а. */
trait GeoShapeQuerable extends GeoShape with IToEsQueryFn {

  /**
    * Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]
    */
  def toEsShapeBuilder: ShapeBuilder

  override def toEsQuery(fn: String): QueryBuilder = {
    QueryBuilders.geoShapeQuery(fn, toEsShapeBuilder)
  }

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

    /** Является ли фигура кругом? У нас редактор кругов отдельно, поэтому и проверка совместимости тут, отдельно. */
    def isCircle: Boolean = false
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
    override def isCircle     = true
  }

  val linestring          : T = new Val("linestring") {
    override def geoJsonName  = Some("LineString")
    override def companion    = LineStringGs
  }

  val multipoint          : T = new Val("multipoint") {
    override def geoJsonName  = Some("MultiPoint")
    override def companion    = MultiPointGs
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

  val envelope            : T = new Val("envelope") {
    override def companion    = EnvelopeGs
  }

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