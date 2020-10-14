package io.suggest.geo

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.common.empty.OptionUtil
import io.suggest.enum2.EnumeratumUtil
import play.api.libs.json.{Format, Reads, Writes}
import play.api.libs.functional.syntax._

object GsType {

  import boopickle.Default._

  /** Поддержка boopickle. */
  implicit def gsTypePickler: Pickler[GsType] = {
    import GsTypes._
    // TODO scala-2.12 Доверить поиск sealed-реализаций автоматике, когда scala-2.11 уйдёт в историю.
    compositePickler[GsType]
      .addConcreteType[Point.type]
      .addConcreteType[Polygon.type]
      .addConcreteType[Circle.type]
      .addConcreteType[LineString.type]
      .addConcreteType[MultiPoint.type]
      .addConcreteType[MultiLineString.type]
      .addConcreteType[MultiPolygon.type]
      .addConcreteType[GeometryCollection.type]
      .addConcreteType[Envelope.type]
  }

  /** Поддержка JSON сериализации/десериализации в JsString. */
  implicit val GS_TYPE_FORMAT: Format[GsType] = {
    EnumeratumUtil.valueEnumEntryFormat(GsTypes)
  }

  /** JSON-форматтер, работающий с GeoJson-типами.
    * Если во writes запихнуть geojson-НЕсовместимый шейп, то будет экзепшен.
    */
  def gsTypeGeoJsonCompatibleFormat: Format[GsTypeGeoJsonCompatible] = {
    val r = EnumeratumUtil._optReads2reads(
      implicitly[Reads[String]]
        .map( GsTypes.withGjValueOpt )
    )

    val w = implicitly[Writes[String]]
      .contramap[GsTypeGeoJsonCompatible]( _.geoJsonName )

    Format(r, w)
  }

}


/** Класс элемента модели [[GsTypes]]. */
sealed abstract class GsType(override val value: String) extends StringEnumEntry {

  /** Название типа на стороне elasticsearch. */
  final def esName: String = value

  /** Имя в рамках спецификации GeoJSON. */
  def geoJsonNameOpt: Option[String] = None

  def isGeoJsonCompatible: Boolean = geoJsonNameOpt.isDefined

  /** Является ли фигура кругом? У нас редактор кругов отдельно, поэтому и проверка совместимости тут, отдельно. */
  def isCircle: Boolean = false

  /** Занимает ли фигура данного типа область на карте (да/нет/зависит от конкретной фигуры)?
    *
    * @return Some(true): Фигура занимает область на карте.
    *         Some(false) Фигура -- это точки/линии, не занимающие места на карте.
    *         None - занимает или нет неизвестно, это зависит от содержимого каждого конкретного шейпа данного типа.
    */
  def isArea: Option[Boolean]

  override final def toString = esName

}


/** Трейт для GeoJson-совместимых реализаций [[GsType]]. */
sealed trait GsTypeGeoJsonCompatible extends GsType {

  /** Имя в рамках спецификации GeoJSON. */
  def geoJsonName: String

  override final def geoJsonNameOpt = Some(geoJsonName)

  override final def isGeoJsonCompatible = true

}


/** Типы плоских фигур, допустимых для отправки в ES для geo-поиска/geo-фильтрации. */
object GsTypes extends StringEnum[GsType] {

  case object Point extends GsType("point") with GsTypeGeoJsonCompatible {
    override def geoJsonName  = "Point"
    override def isArea       = OptionUtil.SomeBool.someFalse
  }

  case object Polygon extends GsType("polygon") with GsTypeGeoJsonCompatible {
    override def geoJsonName  = "Polygon"
    override def isArea       = OptionUtil.SomeBool.someTrue
  }

  case object Circle extends GsType("circle") {
    override def isCircle     = true
    override def isArea       = OptionUtil.SomeBool.someTrue
  }

  case object LineString extends GsType("linestring") with GsTypeGeoJsonCompatible {
    override def geoJsonName  = "LineString"
    override def isArea       = OptionUtil.SomeBool.someFalse
  }

  case object MultiPoint extends GsType("multipoint") with GsTypeGeoJsonCompatible {
    override def geoJsonName  = "MultiPoint"
    override def isArea       = OptionUtil.SomeBool.someFalse
  }

  case object MultiLineString extends GsType("multilinestring") with GsTypeGeoJsonCompatible {
    override def geoJsonName  = "MultiLineString"
    override def isArea       = OptionUtil.SomeBool.someFalse
  }

  case object MultiPolygon extends GsType("multipolygon") with GsTypeGeoJsonCompatible {
    override def geoJsonName  = "MultiPolygon"
    override def isArea       = OptionUtil.SomeBool.someTrue
  }

  case object GeometryCollection extends GsType("geometrycollection") with GsTypeGeoJsonCompatible {
    override def geoJsonName  = "GeometryCollection"
    override def isArea       = None
  }

  case object Envelope extends GsType("envelope") {
    override def isArea       = OptionUtil.SomeBool.someTrue
  }


  override val values = findValues

  /** Карта названий GeoJSON.
    * Её длина всегда меньше values, т.к. Circle и Envelope не поддерживаются в GeoJSON. */
  lazy val geoJsonNamesToValuesMap: Map[String, GsTypeGeoJsonCompatible] = {
    values
      .iterator
      .flatMap {
        case gjGsType: GsTypeGeoJsonCompatible =>
          (gjGsType.geoJsonName -> gjGsType) :: Nil
        case _ =>
          Nil
      }
      .toMap
  }

  // Была поддержка поиска по GeoJSON-имени. TODO Не ясно, нужна ли она сейчас?
  override def withValueOpt(name: String): Option[GsType] = {
    super.withValueOpt(name).orElse {
      withGjValueOpt(name)
    }
  }

  def withGjValueOpt(name: String): Option[GsTypeGeoJsonCompatible] = {
    geoJsonNamesToValuesMap
      .get( name )
  }

}
