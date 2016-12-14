package io.suggest.model.geo

import java.{lang => jl}

import io.suggest.geo.IGeoPoint
import io.suggest.model.es.EsModelUtil
import com.spatial4j.core.context.SpatialContext
import com.spatial4j.core.io.GeohashUtils
import com.spatial4j.core.shape.Point
import com.vividsolutions.jts.geom.Coordinate
import io.suggest.geo.GeoConstants.Qs
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.util.MacroLogsImpl
import org.elasticsearch.common.geo.{GeoPoint => EsGeoPoint}
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.QueryStringBindable
import play.extras.geojson.{LatLng, LngLat}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.08.14 10:40
  * Description: Представление географической точки в рамках проекта s.io.
  * Сделано наподобии org.elasticsearch.common.geo.Geopoint с поправкой на полную неизменяемость и другие фичи.
  *
  * 2016.dec.14: Пошёл вынос класса модели в common, где уже ему было давно заготовлено место.
  * Унификация связана с попыткой использования boopickle на js-стороне.
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
      fromGeoHash(v)
    }
  }

  def apply(esgp: EsGeoPoint): GeoPoint = {
    GeoPoint(lat = esgp.lat,  lon = esgp.lon)
  }

  def apply(point: Point): GeoPoint = {
    GeoPoint(lon = point.getX, lat = point.getY)
  }

  /** Бывает, что идёт выковыривание сырых значений из документов elasticsearch. */
  def fromArraySeq(lonLatColl: TraversableOnce[Any]): Option[GeoPoint] = {
    lazy val logPrefix = s"fromArraySeq(${System.currentTimeMillis()}):"
    val doubleSeq = lonLatColl
      .toIterator
      .flatMap {
        case v: java.lang.Number =>
          val n = JsNumber( v.doubleValue() )
          Seq(n)
        case other =>
          warn(s"$logPrefix Unable to parse agg.value='''$other''' as Number.")
          Nil
      }
      .toSeq
    val jsRes = GeoPoint.READS_ANY
      .reads( JsArray(doubleSeq) )
    if (jsRes.isError)
      error(s"$logPrefix Agg.values parsing failed:\n $jsRes")
    jsRes.asOpt
  }

  def fromLatLon(latLon: String): GeoPoint = {
    val commaInx = getCommaIndex(latLon)
    fromLatLonComma(latLon, commaInx)
  }

  def fromGeoHash(geoHash: String): GeoPoint = {
    val esgp = GeohashUtils.decode(geoHash, SpatialContext.GEO)
    apply(esgp)
  }

  val ES_LAT_FN = "lat"
  val ES_LON_FN = "lon"


  private def getCommaIndex(str: String) = str indexOf ','

  private def fromLatLonComma(latLon: String, commaIndex: Int) = {
    val lat = jl.Double.parseDouble(latLon.substring(0, commaIndex).trim)
    val lon = jl.Double.parseDouble(latLon.substring(commaIndex + 1).trim)
    GeoPoint(
      lat = fixLat(lat),
      lon = fixLon(lon)
    )
  }

  val READS_GEO_ARRAY = Reads[GeoPoint] {
    case JsArray(Seq(lonV, latV)) =>
      val gp = GeoPoint(
        lat = fixLat( latV.as[Double] ),
        lon = fixLon( lonV.as[Double] )
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


  def fixLon(lon: Double): Double = {
    _forceBetween(-180d, lon, 180d)
  }

  def fixLat(lat: Double): Double = {
    _forceBetween(-90d, lat, 90)
  }

  private def _forceBetween(min: Double, value: Double, max: Double): Double = {
    Math.min(max,
      Math.max(min, value)
    )
  }


  /** Поддержка биндинга из/в Query string в play router. */
  implicit def qsb(implicit doubleB: QueryStringBindable[Double]): QueryStringBindable[GeoPoint] = {
    new QueryStringBindableImpl[GeoPoint] {

      override def KEY_DELIM = Qs.DELIM

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, GeoPoint]] = {
        val k = key1F(key)
        for {
          lonEith <- doubleB.bind( k(Qs.LON_FN), params )
          latEith <- doubleB.bind( k(Qs.LAT_FN), params )
        } yield {
          for {
            lon <- lonEith.right
            lat <- latEith.right
          } yield {
            GeoPoint(
              lat = fixLat(lat),
              lon = fixLon(lon)
            )
          }
        }
      }

      override def unbind(key: String, value: GeoPoint): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Seq(
            doubleB.unbind(k(Qs.LON_FN), value.lon),
            doubleB.unbind(k(Qs.LAT_FN), value.lat)
          )
        }
      }
    }
  }

  // Экспорт инстансов IGeoPoint, вынесен из класса GeoPoint перед его депортацией в [common].

  /**
   * Конвертация в GeoJson-представление координат, т.е. в JSON-массив.
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_lat_lon_as_array_5]]
   */
  def toPlayGeoJson(gp: IGeoPoint) = {
    JsArray(Seq(
      JsNumber(gp.lon), JsNumber(gp.lat)
    ))
  }

  def toEsStr(gp: IGeoPoint): String = gp.lat.toString + "," + gp.lon.toString

  /** Пространственная координата в терминах JTS. */
  def toJstCoordinate(gp: IGeoPoint) = new Coordinate(gp.lon, gp.lat)

  def toLngLat(gp: IGeoPoint): LngLat = {
    LngLat(lng = gp.lon, lat = gp.lat)
  }

  def toLatLng(gp: IGeoPoint): LatLng = {
    LatLng(lat = gp.lat, lng = gp.lon)
  }

}


case class GeoPoint(lat: Double, lon: Double) extends IGeoPoint {

  override def toString: String = super.toString

}
