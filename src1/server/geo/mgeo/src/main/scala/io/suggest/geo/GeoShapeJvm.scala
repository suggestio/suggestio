package io.suggest.geo

import io.suggest.util.JacksonParsing.FieldsJsonAcc
import io.suggest.util.logs.MacroLogsDyn
import org.elasticsearch.common.geo.builders.ShapeBuilder
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import play.api.libs.json._
import au.id.jazzy.play.geojson.{Geometry, LngLat}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.14 9:43
  * Description: GeoShape - система задания'ов, которые могут быть различными геометрическими фиругами.
  * В функциях модуля избегаем использования jts из-за возможных проблем с XYZ-координатами в будущем.
  *
  * Эта модель служит цели
  */

object GeoShapeJvm extends MacroLogsDyn {

  val COORDS_ESFN = "coordinates"
  val TYPE_ESFN   = "type"

  val TYPE_FORMAT = (__ \ TYPE_ESFN).format[GsType]


  val READS = Reads[IGeoShape] {
    case o: JsObject =>
      o.validate(TYPE_FORMAT)
        .flatMap { gsType =>
          o.validate( GsTypesJvm.companionFor(gsType).DATA_FORMAT )
        }

    case other =>
      JsError( JsonValidationError("expected.jsobject", other) )
  }

  val WRITES = Writes[IGeoShape] { gs =>
    // TODO Надо задействовать companion.DATA_FORMAT, как это в READS сделано, связав его с TYPE_FORMAT тут.
    // И тогда метод toPlayJson() можно будет выкинуть окончательно.
    toPlayJson( gs, geoJsonCompatible = false )
  }

  implicit val GEO_SHAPE_FORMAT = Format(READS, WRITES)

  /** Распарсить шейп из json-выхлопа. */
  def parse(raw: String): IGeoShape = {
    Json.parse(raw).as[IGeoShape]
  }

  def WRITES_GJSON_COMPAT: OWrites[IGeoShape] = {
    OWrites { v =>
      toPlayJson( v, geoJsonCompatible = true )
    }
  }

  def FORMAT_GJSON_COMPAT: OFormat[IGeoShape] = {
    OFormat(READS, WRITES_GJSON_COMPAT)
  }

  def toPlayGeoJsonGeom(gs: IGeoShape): Geometry[LngLat] = {
    val c = GsTypesJvm.companionFor(gs.shapeType)
    // TODO Нужна higher-kinds метод, занимающийся этим без asInstanceOf.
    c.toPlayGeoJsonGeom( gs.asInstanceOf[c.Shape_t] )
  }

  /** Отрендерить json для сохранения внутри _source. */
  // TODO От этого кривого метода зависит только этот WRITES. Надо бы его спилить, и сделать нормальный WRITES.
  def toPlayJson(gs: IGeoShape, geoJsonCompatible: Boolean = false): JsObject = {
    val c = GsTypesJvm.companionFor( gs.shapeType )
    c.toPlayJson(gs.asInstanceOf[c.Shape_t], geoJsonCompatible)
  }

  /**
    * Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]
    */
  def toEsShapeBuilder(gs: IGeoShapeQuerable): ShapeBuilder = {
    val c = GsTypesJvm.companionFor( gs.shapeType )
      .asInstanceOf[GsStaticJvmQuerable]
    c.toEsShapeBuilder( gs.asInstanceOf[c.Shape_t] )
  }

  def toEsQueryMaker(gs: IGeoShapeQuerable): IToEsQueryFn = {
    val gsb = toEsShapeBuilder(gs)
    new IToEsQueryFn {
      override def toEsQuery(fn: String): QueryBuilder = {
        QueryBuilders.geoShapeQuery(fn, gsb)
      }
    }
  }

}



/** Интерфейс для объекта-компаньона на стороне JVM. */
trait GsStaticJvm {

  type Shape_t <: IGeoShape

  def DATA_FORMAT: Reads[Shape_t]

  /** Конвертация в play.extras.geojson.Geomenty.
    * Circle конвертится в точку!
    * ES envelope -- пока не поддерживается, но можно представить прямоугольным полигоном.
    * @param gs Шейп.
    * @return Геометрия play-geojson.
    */
  def toPlayGeoJsonGeom(gs: Shape_t): Geometry[LngLat]


  /** Отрендерить json для сохранения внутри _source. */
  def toPlayJson(gs: Shape_t, geoJsonCompatible: Boolean = false): JsObject = {
    val typeName: String = if (geoJsonCompatible) {
      gs.shapeType.geoJsonName.get
    } else {
      gs.shapeType.esName
    }
    val kv = GeoShapeJvm.TYPE_ESFN -> JsString(typeName)
    val acc = kv :: _toPlayJsonInternal(gs, geoJsonCompatible)
    JsObject(acc)
  }

  /** Фигуро-специфический рендер JSON для значения внутри _source. */
  protected[this] def _toPlayJsonInternal(gs: Shape_t, geoJsonCompatible: Boolean): FieldsJsonAcc

}


/** Статическая поддержка querable-шейпов, пригодных для сборки search query запроса в ES.
  * Примитивные гео-шейпы являются пригодными для es query, но вот [[GeometryCollectionGs]] -- нет.  */
trait GsStaticJvmQuerable extends GsStaticJvm {

  override type Shape_t <: IGeoShapeQuerable

  /**
    * Отрендерить в изменяемый ShapeBuilder для построения ES-запросов.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-geo-shape-query.html]]
    */
  def toEsShapeBuilder(gs: Shape_t): ShapeBuilder

  def toEsQueryMaker(gs: Shape_t): IToEsQueryFn = {
    val gsb = toEsShapeBuilder(gs)
    new IToEsQueryFn {
      override def toEsQuery(fn: String): QueryBuilder = {
        QueryBuilders.geoShapeQuery(fn, gsb)
      }
    }
  }
}
