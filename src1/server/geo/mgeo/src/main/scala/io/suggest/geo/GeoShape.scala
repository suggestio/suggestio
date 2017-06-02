package io.suggest.geo

import io.suggest.util.JacksonParsing.FieldsJsonAcc
import io.suggest.util.logs.MacroLogsDyn
import org.elasticsearch.common.geo.builders.ShapeBuilder
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.extras.geojson.{Geometry, LngLat}
import GsTypesJvm.GS_TYPE_FORMAT

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
          o.validate( GsTypesJvm.companionFor(gsType).DATA_FORMAT )
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

  def toPlayGeoJsonGeom(gs: GeoShape): Geometry[LngLat] = {
    val c = GsTypesJvm.companionFor(gs.shapeType)
    // TODO Нужна higher-kinds метод, занимающийся этим без asInstanceOf.
    c.toPlayGeoJsonGeom( gs.asInstanceOf[c.Shape_t] )
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



/** Интерфейс для объекта-компаньона на стороне JVM. */
trait GsStaticJvm {

  type Shape_t <: GeoShape

  def DATA_FORMAT: Reads[Shape_t]

  /** Конвертация в play.extras.geojson.Geomenty.
    * Circle конвертится в точку!
    * ES envelope -- пока не поддерживается, но можно представить прямоугольным полигоном.
    * @param gs Шейп.
    * @return Геометрия play-geojson.
    */
  def toPlayGeoJsonGeom(gs: Shape_t): Geometry[LngLat]

}
