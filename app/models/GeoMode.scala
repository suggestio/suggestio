package models

import java.net.InetAddress
import io.suggest.ym.model.common.{Distance, GeoDistanceQuery}
import org.elasticsearch.common.unit.DistanceUnit
import play.api.cache.Cache
import play.api.db.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.RequestHeader
import play.api.Play.{current, configuration}
import util.PlayMacroLogsImpl
import scala.concurrent.{Future, future}
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.14 10:31
 * Description: Режимы геолокации и утиль для них.
 */

object GeoMode {

  /** Регэксп для извлечения координат из строки, переданной веб-мордой. */
  val LAT_LON_RE = """-?\d{1,3}\.\d{0,8},-?\d{1,3}\.\d{0,8}""".r

  /** Распарсить опциональное сырое значение qs-параметра a.geo=. */
  def apply(raw: Option[String]): GeoMode = {
    raw.fold [GeoMode] (GeoNone) {
      case "ip"  => GeoIp
      case LAT_LON_RE(latStr, lonStr) =>
        GeoLocation(latStr.toDouble, lonStr.toDouble)
      case other => GeoNone
    }
  }
}


/** Интерфейс для режимов геопоиска. */
sealed trait GeoMode {
  /** Запрошена ли геолокация вообще? */
  def isWithGeo: Boolean

  /** Конвертация значения геолокации обратно в выхлоп QueryStringBindable[Option[String]. */
  def toQsStringOpt: Option[String]

  /**
   * Запуск определения местоположения клиента.
   * @param request Текущий реквест.
   * @return Фьючерс с результатом, пригодным для отправки в модели, поддерживающие геолокацию.
   */
  def geoSearchInfo(implicit request: RequestHeader): Future[Option[GeoSearchInfo]]

  def exactGeodata: Option[GeoPoint]
}


trait GeoSearchInfo {
  def geoPoint: GeoPoint
  def geoDistanceQuery: GeoDistanceQuery
  def cityName: Option[String]
  def countryIso2: Option[String]
  def exactGeopoint: Option[GeoPoint]
  def ipGeopoint: Option[GeoPoint]
}


/** Геолокация НЕ включена. */
case object GeoNone extends GeoMode {
  override def isWithGeo = false
  override def toQsStringOpt = None
  override def geoSearchInfo(implicit request: RequestHeader): Future[Option[GeoSearchInfo]] = {
    Future successful None
  }
  override def exactGeodata = None
}


/** Геолокация по ip. */
case object GeoIp extends GeoMode with PlayMacroLogsImpl {

  import LOGGER._

  val CACHE_TTL_SECONDS = configuration.getInt("geo.ip.cache.ttl.seconds") getOrElse 240

  val DISTANCE_KM_DFLT: Double = configuration.getDouble("geo.ip.distance.km.dflt") getOrElse 50.0

  def DISTANCE_DFLT = Distance(DISTANCE_KM_DFLT, DistanceUnit.KILOMETERS)

  val REPLACE_LOCALHOST_IP_WITH: String = configuration.getString("geo.ip.localhost.replace.with") getOrElse "213.108.35.158"

  override def isWithGeo = true
  override def toQsStringOpt = Some("ip")
  override def geoSearchInfo(implicit request: RequestHeader): Future[Option[GeoSearchInfo]] = {
    // Запускаем небыстрый синхронный поиск в отдельном потоке.
    future {
      val ra = getRemoteAddr
      ip2rangeCity(ra) map { result =>
        new GeoSearchInfo {
          override def geoPoint = result.city.geoPoint
          override def geoDistanceQuery = {
            GeoDistanceQuery(
              center = result.city.geoPoint,
              distanceMin = None,
              distanceMax = DISTANCE_DFLT
            )
          }
          override def cityName = Option(result.city.cityName)
          override def countryIso2 = Option(result.range.countryIso2)
          override def exactGeopoint = None
          override def ipGeopoint = Option(geoPoint)
        }
      }
    }     // future()
  }

  case class Ip2RangeResult(city: IpGeoBaseCity, range: IpGeoBaseRange)

  def getRemoteAddr(implicit request: RequestHeader): String = {
    val ra0 = request.remoteAddress
    // Если это локальный адрес, то нужно его подменить на адрес офиса cbca. Это нужно для облегчения отладки.
    val inetAddr = InetAddress.getByName(ra0)
    if (inetAddr.isAnyLocalAddress || inetAddr.isLoopbackAddress) {
      val ra1 = REPLACE_LOCALHOST_IP_WITH
      debug(s"getRemoteAddr(): Local ip detected: $ra0. Rewriting ip with $ra1")
      ra1
    } else {
      ra0
    }
  }

  def ip2rangeCity(ip: String): Option[Ip2RangeResult] = {
    // Операция поиска ip в SQL-базе ресурсоёмкая, поэтому кешируем результат.
    val ck = ip + ".gipq"
    Cache.getOrElse(ck, CACHE_TTL_SECONDS) {
      val result = DB.withConnection { implicit c =>
        IpGeoBaseRange.findForIp(InetAddress getByName ip)
          .headOption
          .flatMap { ipRange =>
          ipRange.cityOpt map { city =>
            Ip2RangeResult(city, ipRange)
          }
        }

      } // DB
      lazy val logPrefix = s"ip2rangeCity($ip): "
      if (result.isEmpty) {
        warn(logPrefix + "IP not found in ipgeobase.")
      } else {
        trace(logPrefix + "Candidate city: " + result.get.city.cityName)
      }
      result
    }   // Cache
  }

  override def exactGeodata: Option[GeoPoint] = None
}


object GeoLocation {

  val DISTANCE_KM_DFLT: Double = configuration.getDouble("geo.location.distance.km.dflt") getOrElse 15.0

  def DISTANCE_DFLT = Distance(DISTANCE_KM_DFLT, DistanceUnit.KILOMETERS)

}


/** Геолокация с указанием географических координат. */
case class GeoLocation(lat: Double, lon: Double) extends GeoMode { gl =>
  lazy val geopoint = GeoPoint(lat = lat, lon = lon)

  override def isWithGeo = true
  override def toQsStringOpt = Some(s"$lat,$lon")

  override def geoSearchInfo(implicit request: RequestHeader): Future[Option[GeoSearchInfo]] = {
    val result = new GeoSearchInfo {
      override def geoPoint: GeoPoint = gl.geopoint
      override def geoDistanceQuery = GeoDistanceQuery(
        center      = gl.geopoint,
        distanceMin = None,
        distanceMax = GeoLocation.DISTANCE_DFLT
      )
      override def exactGeopoint = Some(gl.geopoint)
      lazy val ipGeoloc = {
        val ra = GeoIp.getRemoteAddr
        GeoIp.ip2rangeCity(ra)
      }
      override def ipGeopoint: Option[GeoPoint] = {
        ipGeoloc map { _.city.geoPoint }
      }
      override def cityName = ipGeoloc.map(_.city.cityName)
      override def countryIso2 = ipGeoloc.map(_.range.countryIso2)
    }
    Future successful Some(result)
  }

  override def exactGeodata = Some(geopoint)
}
