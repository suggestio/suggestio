package io.suggest.geo

import enumeratum._
import io.suggest.enum2.EnumeratumUtil
import play.api.libs.json.Format

object GsType {

  import boopickle.Default._

  /** Поддержка boopickle. */
  implicit val gsTypePickler: Pickler[GsType] = {
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
    EnumeratumUtil.enumEntryFormat(GsTypes)
  }

}


/** Класс элемента модели [[GsTypes]]. */
sealed abstract class GsType extends EnumEntry {

  /** Название типа на стороне elasticsearch. */
  def esName: String

  /** Имя в рамках спецификации GeoJSON. */
  def geoJsonName: Option[String]

  def isGeoJsonCompatible: Boolean = geoJsonName.isDefined

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


/** Типы плоских фигур, допустимых для отправки в ES для geo-поиска/geo-фильтрации. */
object GsTypes extends Enum[GsType] {

  case object Point extends GsType {
    override def esName       = "point"
    override def geoJsonName  = Some("Point")
    override def isArea       = Some(false)
  }

  case object Polygon extends GsType {
    override def esName       = "polygon"
    override def geoJsonName  = Some("Polygon")
    override def isArea       = Some(true)
  }

  case object Circle extends GsType {
    override def esName       = "circle"
    override def isCircle     = true
    override def geoJsonName  = None
    override def isArea       = Some(true)
  }

  case object LineString extends GsType {
    override def esName       = "linestring"
    override def geoJsonName  = Some("LineString")
    override def isArea       = Some(false)
  }

  case object MultiPoint extends GsType {
    override def esName       = "multipoint"
    override def geoJsonName  = Some("MultiPoint")
    override def isArea       = Some(false)
  }

  case object MultiLineString extends GsType {
    override def esName       = "multilinestring"
    override def geoJsonName  = Some("MultiLineString")
    override def isArea       = Some(false)
  }

  case object MultiPolygon extends GsType {
    override def esName       = "multipolygon"
    override def geoJsonName  = Some("MultiPolygon")
    override def isArea       = Some(true)
  }

  case object GeometryCollection extends GsType {
    override def esName       = "geometrycollection"
    override def geoJsonName  = Some("GeometryCollection")
    override def isArea       = None
  }

  case object Envelope extends GsType {
    override def esName       = "envelope"
    override def geoJsonName  = None
    override def isArea       = Some(true)
  }


  override val values = findValues

  /** Карта названий GeoJSON.
    * Её длина всегда меньше values, т.к. Circle и Envelope не поддерживаются в GeoJSON. */
  lazy val geoJsonNamesToValuesMap: Map[String, GsType] = {
    val iter = for {
      v       <- values.iterator
      gjName  <- v.geoJsonName
    } yield {
      gjName -> v
    }
    iter.toMap
  }

  // Была поддержка поиска по GeoJSON-имени. TODO Не ясно, нужна ли она сейчас?
  override def withNameOption(name: String): Option[GsType] = {
    super.withNameOption(name).orElse {
      geoJsonNamesToValuesMap.get( name )
    }
  }

}
