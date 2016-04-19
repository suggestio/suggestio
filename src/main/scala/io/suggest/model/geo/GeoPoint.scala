package io.suggest.model.geo

import java.{lang => jl, util => ju}

import io.suggest.geo.IGeoPoint
import io.suggest.model.es.EsModelUtil
import EsModelUtil.doubleParser
import com.vividsolutions.jts.geom.Coordinate
import io.suggest.util.{JacksonWrapper, MacroLogsImpl}
import org.elasticsearch.common.geo.{GeoHashUtils, GeoPoint => EsGeoPoint}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.extras.geojson.{LatLng, LngLat}

import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.08.14 10:40
 * Description: Представление географической точки в рамках проекта s.io.
 * Сделано наподобии org.elasticsearch.common.geo.Geopoint с поправкой на полную неизменяемость и другие фичи.
 *
 * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html]]
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

  val ES_LAT_FN = "lat"
  val ES_LON_FN = "lon"


  /** Десериализатор гео-точки из представления ES, распарсенного через Jackson. */
  // TODO Надо задействовать play-json десериализаторы, они более православны, чем этот велосипед.
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
      Option(jm get ES_LAT_FN) flatMap { latRaw =>
        Option(jm get ES_LON_FN) flatMap { lonRaw =>
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

  val READS_GEO_ARRAY = Reads[GeoPoint] {
    case JsArray(Seq(lonV, latV)) =>
      val gp = GeoPoint(
        lat = latV.as[Double],
        lon = lonV.as[Double]
      )
      JsSuccess(gp)
    case other =>
      JsError( ValidationError("expected.jsarray", other) )
  }

  val WRITES_GEO_ARRAY = Writes[GeoPoint] { gp =>
    JsArray(
      Seq(
        JsNumber(gp.lon),
        JsNumber(gp.lat)
      )
    )
  }

  /** Десериализация из js-массива вида [-13.22,45.34]. */
  val FORMAT_GEO_ARRAY = Format[GeoPoint](READS_GEO_ARRAY, WRITES_GEO_ARRAY)

  /** Десериализация из строки вида "45.34,-13.22". */
  val READS_STRING = Reads[GeoPoint] {
    case JsString(raw) =>
      JsSuccess( apply(raw) )
    case other =>
      JsError( ValidationError("expected.jsstring", other) )
  }

  /** JSON-формат для ввода-вывода в виде JSON-объекта с полями lat и lon. */
  val FORMAT_OBJECT: Format[GeoPoint] = (
    (__ \ ES_LAT_FN).format[Double] and
    (__ \ ES_LON_FN).format[Double]
  )(apply(_, _), unlift(unapply))

  /** Десериализация из JSON из различных видов представления геоточки. */
  val READS_ANY: Reads[GeoPoint] = {
    FORMAT_GEO_ARRAY
      .orElse( FORMAT_OBJECT )
      .orElse( READS_STRING )
  }

  /** Дефолтовый JSON-форматтер для десериализации из разных форматов,
    * но сериализации в JSON object с полями lat и lon. */
  implicit val FORMAT_ANY_TO_ARRAY: Format[GeoPoint] = {
    Format[GeoPoint](READS_ANY, FORMAT_GEO_ARRAY)
  }

}


case class GeoPoint(lat: Double, lon: Double) extends IGeoPoint {

  /** Конвертация в экземпляр ES GeoPoint. */
  def toEsGeopoint = new EsGeoPoint(lat, lon)

  /** Конвертация в геохеш. */
  def geohash = GeoHashUtils.encode(lat, lon)

  override def toString: String = super.toString

  /**
   * Конвертация в GeoJson-представление координат, т.е. в JSON-массив.
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_lat_lon_as_array_5]]
   */
  def toPlayGeoJson = {
    JsArray(Seq(
      JsNumber(lon), JsNumber(lat)
    ))
  }

  /** elasticseearch-представление. */
  def toEsStr: String = toQsStr
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

  def toLatLng: LatLng = {
    LatLng(lat = lat, lng = lon)
  }

  def toLngLat: LngLat = {
    LngLat(lng = lon, lat = lat)
  }

  /** Пространственная координата в терминах JTS. */
  def toJstCoordinate = new Coordinate(lon, lat)

}
