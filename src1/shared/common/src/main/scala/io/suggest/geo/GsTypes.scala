package io.suggest.geo

import enumeratum._


sealed abstract class GsType extends EnumEntry {

  /** Название типа на стороне elasticsearch. */
  def esName: String

  /** Имя в рамках спецификации GeoJSON. */
  def geoJsonName: Option[String]

  def isGeoJsonCompatible: Boolean = geoJsonName.isDefined

  /** Является ли фигура кругом? У нас редактор кругов отдельно, поэтому и проверка совместимости тут, отдельно. */
  def isCircle: Boolean = false

  override final def toString = esName

}


/** Типы плоских фигур, допустимых для отправки в ES для geo-поиска/geo-фильтрации. */
object GsTypes extends Enum[GsType] {

  case object Point extends GsType {
    override def esName       = "point"
    override def geoJsonName  = Some("Point")
  }

  case object Polygon extends GsType {
    override def esName       = "polygon"
    override def geoJsonName  = Some("Polygon")
  }

  case object Circle extends GsType {
    override def esName       = "circle"
    override def isCircle     = true
    override def geoJsonName  = None
  }

  case object LineString extends GsType {
    override def esName       = "linestring"
    override def geoJsonName  = Some("LineString")
  }

  case object MultiPoint extends GsType {
    override def esName       = "multipoint"
    override def geoJsonName  = Some("MultiPoint")
  }

  case object MultiLineString extends GsType {
    override def esName       = "multilinestring"
    override def geoJsonName  = Some("MultiLineString")
  }

  case object MultiPolygon extends GsType {
    override def esName       = "multipolygon"
    override def geoJsonName  = Some("MultiPolygon")
  }

  case object GeometryCollection extends GsType {
    override def esName       = "geometrycollection"
    override def geoJsonName  = Some("GeometryCollection")
  }

  case object Envelope extends GsType {
    override def esName = "envelope"
    override def geoJsonName = None
  }


  override def withNameOption(name: String): Option[GsType] = {
    super.withNameOption(name).orElse {
      values.find(_.geoJsonName.contains(name))
    }
  }

  override val values = findValues

}
