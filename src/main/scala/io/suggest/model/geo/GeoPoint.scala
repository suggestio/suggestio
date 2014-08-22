package io.suggest.model.geo

import java.{lang => jl, util => ju}

import io.suggest.model.EsModel.doubleParser
import io.suggest.util.{JacksonWrapper, MacroLogsImpl}
import org.elasticsearch.common.geo.{GeoHashUtils, GeoPoint => EsGeoPoint}
import play.api.libs.json._

import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.08.14 10:40
 * Description: Представление географической точки в рамках проекта s.io.
 * Сделано наподобии org.elasticsearch.common.geo.Geopoint с поправкой на полную неизменяемость и другие фичи.
 */

object GeoPoint extends MacroLogsImpl {

  import LOGGER._

  def apply(v: String): GeoPoint = {
    val commaIndex = getCommaIndex(v)
    if (commaIndex != -1) {
      fromLatLonComma(v, commaIndex)
    } else {
      fromGeohash(v)
    }
  }

  implicit def apply(esgp: EsGeoPoint): GeoPoint = {
    GeoPoint(lat = esgp.lat,  lon = esgp.lon)
  }

  def fromGeohash(geohash: String): GeoPoint = {
    val esgp = GeoHashUtils.decode(geohash)
    GeoPoint(esgp)
  }

  def fromLatLon(latLon: String): GeoPoint = {
    val commaInx = getCommaIndex(latLon)
    fromLatLonComma(latLon, commaInx)
  }


  /** Десериализатор гео-точки из представления ES, распарсенного через Jackson. */
  val deserializeOpt: PartialFunction[Any, Option[GeoPoint]] = {
    // Массив GeoJson в формате [LON, lat]: [12.53245, -44.43553] -- http://geojson.org/
    case l: jl.Iterable[_] =>
      deserializeOpt(l: Traversable[_])

    case l: Traversable[_] =>
      l.headOption.flatMap { lonRaw =>
        l.tail.headOption.flatMap { latRaw =>
          try {
            val lat = doubleParser(latRaw)
            val lon = doubleParser(lonRaw)
            Some(GeoPoint(lat = lat, lon = lon))
          } catch {
            case ex: Throwable =>
              error("Cannot deserialize geojson array into GeoPoint: [" + l.mkString(", ") + "]", ex)
              None
          }
        }
      }
    // Строковое представление вида "41.12,-71.34" ("lat,lon")
    case s: String =>
      try {
        Some(GeoPoint(s))
      } catch {
        case ex: Throwable =>
          error("Cannot deserialize GeoPoint from string: " + s, ex)
          None
      }
    // {"lat": 12.123, "lon": -41.542}
    case jm: ju.Map[_,_] =>
      Option(jm get "lat") flatMap { latRaw =>
        Option(jm get "lon") flatMap { lonRaw =>
          try {
            val lat = doubleParser(latRaw)
            val lon = doubleParser(lonRaw)
            val gp = GeoPoint(lat = lat, lon = lon)
            Some(gp)
          } catch {
            case ex: Throwable =>
              error("Cannot deserialize GeoPoint from JSON object:\n" + JacksonWrapper.serializePretty(jm), ex)
              None
          }
        }
      }
  }

  private def getCommaIndex(str: String) = str indexOf ','

  private def fromLatLonComma(latLon: String, commaIndex: Int) = {
    val lat = jl.Double.parseDouble(latLon.substring(0, commaIndex).trim)
    val lon = jl.Double.parseDouble(latLon.substring(commaIndex + 1).trim)
    GeoPoint(lat = lat, lon = lon)
  }
}


case class GeoPoint(lat: Double, lon: Double) {

  /** Конвертация в экземпляр ES GeoPoint. */
  def toEsGeopoint = new EsGeoPoint(lat, lon)

  /** Конвертация в геохеш. */
  def geohash = GeoHashUtils.encode(lat, lon)

  override def toString: String = s"[$lat, $lon]"

  /**
   * Конвертация в GeoJson-представление координат, т.е. в JSON-массив.
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_lat_lon_as_array_5]]
   */
  def toPlayGeoJson = {
    JsArray(Seq(
      JsNumber(lon), JsNumber(lat)
    ))
  }

  /**
   * Конвертация в строковое ES-представление координат: "-12.453,23.243"
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_lat_lon_as_string_6]]
   */
  def toEsStr = s"$lat,$lon"
  def toPlayJsonEsStr = JsString(toEsStr)

  /**
   * Конвертация координат в play JSON представление.
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_lat_lon_as_properties_5]]
   */
  def toPlayJson = {
    JsObject(Seq(
      "lat" -> JsNumber(lat),
      "lon" -> JsNumber(lon)
    ))
  }

}
