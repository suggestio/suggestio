package models

import java.net.InetAddress
import io.suggest.geo.GeoConstants
import io.suggest.model.geo
import io.suggest.model.geo.{GeoDistanceQuery, Distance}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.elasticsearch.common.unit.DistanceUnit
import play.api.http.HeaderNames
import play.api.mvc.QueryStringBindable
import play.api.Play.{current, configuration}
import util.acl.SioRequestHeader
import util.async.AsyncUtil
import util.xplay.CacheUtil
import util.{PlayLazyMacroLogsImpl, PlayMacroLogsImpl}
import scala.concurrent.Future
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.14 10:31
 * Description: Режимы геолокации и утиль для них.
 */

object GeoMode extends PlayLazyMacroLogsImpl with JavaTokenParsers {

  import LOGGER._

  val latLonNumberRe = """-?\d{1,3}(\.(\d{0,20})?)?""".r
  val accurNumberRe = """[-+]?\d{1,5}(\.(\d{0,20})?)?""".r
  val delimRe = "[,;]".r
  val ipModeRe = "(?i)ip".r

  /** Ленивый потоко-небезопасный (вроде бы) парсер координат из строки. */
  def latLonAccurP: Parser[GeoLocation] = {
    val doubleP = latLonNumberRe ^^ { _.toDouble }
    val delimP: Parser[_] = delimRe
    // Аккуратность в метрах -- может отсутствовать, может быть неправильной (null?)
    val accurOptP: Parser[Option[Double]] = {
      opt(
        delimP ~> (accurNumberRe ^^ (s => Some(s.toDouble)) | ("(?i)[^,;]*".r ^^^ None))
      ) ^^ {
        _.flatten
      }
    }
    val latLonP = ((doubleP <~ delimP) ~ doubleP) ^^ {
      case lat ~ lon  =>  GeoPoint(lat = lat, lon = lon)
    }
    (latLonP ~ accurOptP) ^^ {
      case gp ~ accurOpt  =>  GeoLocation(gp, accuracyMeters = accurOpt)
    }
  }

  /** Логический аналог, Option[]. При проблемах возвращает None. */
  def maybeApply(rawOpt: Option[String]): Option[GeoMode] = {
    rawOpt.flatMap { raw =>
      val ipP = ipModeRe ^^^ GeoIp
      val p: Parser[GeoMode] = ipP | latLonAccurP | ("" ^^^ GeoNone)
      val pr = parse(p, raw)
      if (pr.successful) {
        Some(pr.get)
      } else {
        warn(s"maybeApply(): Unknown .geo format: $raw - fallback to None.")
        None
      }
    }
  }

  /** Распарсить опциональное сырое значение qs-параметра a.geo=. */
  def apply(raw: Option[String]): GeoMode = {
    maybeApply(raw) getOrElse GeoNone
  }

  /** Биндер для набега на GeoMode, сериализованный в qs. */
  implicit def geoModeQsb(implicit strOptB: QueryStringBindable[Option[String]]): QueryStringBindable[GeoMode] = {
    import util.qsb.QsbUtil._

    new QueryStringBindable[GeoMode] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, GeoMode]] = {
        for {
          maybeGeo <- strOptB.bind(key, params)
        } yield {
          maybeApply(maybeGeo).fold [Either[String,GeoMode]] {Left("error.unknown")} { Right.apply }
        }
      }

      override def unbind(key: String, value: GeoMode): String = {
        strOptB.unbind(key, value.toQsStringOpt)
      }
    }
  }

  implicit def eitherGeoMode2gm(e: Either[String, GeoMode]): GeoMode = {
    e.right getOrElse GeoNone
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
  def geoSearchInfoOpt(implicit request: SioRequestHeader): Future[Option[GeoSearchInfo]]

  def exactGeodata: Option[geo.GeoPoint]

  /** Уровни, по которым надо искать. */
  def nodeDetectLevels: Seq[NodeGeoLevel]

  def asGeoLocation: Option[GeoLocation] = None

  def isExact: Boolean
}


trait GeoSearchInfo {
  /** Географическая точка, заданная координатами и описывающая клиента. */
  def geoPoint: geo.GeoPoint
  /** Сборка запроса для геопоиска относительно точки.. */
  def geoDistanceQuery: GeoDistanceQuery
  /** Название города. */
  def cityName: Option[String]
  /** Двухбуквенный код страны. */
  def countryIso2: Option[String]
  /** Точная геолокация клиента, если есть. */
  def exactGeopoint: Option[geo.GeoPoint]
  /** Координаты точки, которая набегает */
  def ipGeopoint: Option[geo.GeoPoint]
  /** Является ли браузер клиента частью cbca? */
  def isLocalClient: Boolean
}


/** Геолокация НЕ включена. */
case object GeoNone extends GeoMode {
  override def isWithGeo = false
  override def toQsStringOpt = None
  override def geoSearchInfoOpt(implicit request: SioRequestHeader): Future[Option[GeoSearchInfo]] = {
    Future successful None
  }
  override def exactGeodata = None

  /** Отсутвие геолокации означает отсутсвие уровней оной. */
  override def nodeDetectLevels = Nil

  override def isExact: Boolean = false
}


/** Геолокация по ip. */
case object GeoIp extends GeoMode with PlayMacroLogsImpl {

  import LOGGER._

  val CACHE_TTL_SECONDS = configuration.getInt("geo.ip.cache.ttl.seconds") getOrElse 240

  val DISTANCE_KM_DFLT: Double = configuration.getDouble("geo.ip.distance.km.dflt") getOrElse 50.0

  def DISTANCE_DFLT = Distance(DISTANCE_KM_DFLT, DistanceUnit.KILOMETERS)

  val REPLACE_LOCALHOST_IP_WITH: String = configuration.getString("geo.ip.localhost.replace.with") getOrElse "213.108.35.158"

  /** Выставлять флаг local client, если User-Agent содержит подстроку, подходящую под этот регэксп. */
  val LOCAL_CL_UA_RE = "(?i)(NCDN|ngenix)".r

  override def isWithGeo = true
  override def toQsStringOpt = Some(GeoConstants.GEO_MODE_IP)
  override def geoSearchInfoOpt(implicit request: SioRequestHeader): Future[Option[GeoSearchInfo]] = {
    // Запускаем небыстрый синхронный поиск в отдельном потоке.
    val ra = getRemoteAddr
    ip2rangeCity(ra).map { resultOpt =>
      resultOpt.map { result =>
        new GeoSearchInfo {
          private def ipGeoPoint = result.city.geoPoint
          override def geoPoint = ipGeoPoint
          override def geoDistanceQuery = {
            GeoDistanceQuery(
              center = ipGeoPoint,
              distanceMin = None,
              distanceMax = DISTANCE_DFLT
            )
          }
          override def cityName = Option(result.city.cityName)
          override def countryIso2 = Option(result.range.countryIso2)
          override def exactGeopoint = None
          override def ipGeopoint = Option(geoPoint)
          override lazy val isLocalClient = {
            ra == REPLACE_LOCALHOST_IP_WITH || {
              request.headers
                .get(HeaderNames.USER_AGENT)
                .exists { LOCAL_CL_UA_RE.pattern.matcher(_).find() }
            }
          }
        }
      }
    }
  }

  override def isExact: Boolean = false

  case class Ip2RangeResult(city: IpGeoBaseCity, range: IpGeoBaseRange)

  def getRemoteAddr(implicit request: SioRequestHeader): String = {
    val ra0 = request.remoteAddress
    // Если это локальный адрес, то нужно его подменить на адрес офиса cbca. Это нужно для облегчения отладки.
    // Обёрнуто в try чтобы подавлять сюрпризы содержимого remoteAddress.
    try {
      val inetAddr = InetAddress.getByName(ra0)
      if (inetAddr.isAnyLocalAddress || inetAddr.isSiteLocalAddress || inetAddr.isLoopbackAddress) {
        val ra1 = REPLACE_LOCALHOST_IP_WITH
        debug(s"getRemoteAddr(): Local ip detected: $ra0. Rewriting ip with $ra1")
        ra1
      } else {
        ra0
      }
    } catch {
      case ex: Exception =>
        error("getRemoteAddr(): Failed to parse remote address from " + ra0, ex)
        ra0
    }
  }

  /** Асинхронный поиск какого-то ip в базе ip-адресов.
    * @param ip строка ip-адреса. */
  def ip2rangeCity(ip: String): Future[Option[Ip2RangeResult]] = {
    // Операция поиска ip в SQL-базе ресурсоёмкая, поэтому кешируем результат.
    CacheUtil.getOrElse(ip + ".gipq", CACHE_TTL_SECONDS) {
      val ipAddr = InetAddress getByName ip
      AsyncUtil.jdbcAsync { implicit c =>
        IpGeoBaseRange.findForIp(ipAddr)
      } map {
        _.headOption
      } flatMap {
        case Some(range) =>
          AsyncUtil.jdbcAsync { implicit c =>
            range.cityOpt
          } map { cityOpt =>
            cityOpt.map { city =>
              Ip2RangeResult(city, range)
            }
          }
        case None =>
          Future successful None
      }
    }
  }

  override def exactGeodata: Option[geo.GeoPoint] = None

  override def nodeDetectLevels = Seq(NodeGeoLevels.NGL_TOWN)
}


object GeoLocation {

  /** Дефолтовый радиус обнаружения пользователя, для которого известны координаты. */
  val ES_DISTANCE_DFLT = {
    val raw = configuration.getString("geo.location.distance.dflt") getOrElse "15m"
    DistanceUnit.Distance.parseDistance(raw)
  }

  val DISTANCE_DFLT = Distance(ES_DISTANCE_DFLT)

}


/** Геолокация с указанием географических координат.
  * @param geoPoint Точка (координаты).
  * @param accuracyMeters Необязательная точность в метрах.
  */
final case class GeoLocation(geoPoint: GeoPoint, accuracyMeters: Option[Double] = None) extends GeoMode { gl =>

  override def isWithGeo = true
  override def toQsStringOpt = Some(geoPoint.toQsStr)

  override def geoSearchInfoOpt(implicit request: SioRequestHeader): Future[Option[GeoSearchInfo]] = {
    val ra = GeoIp.getRemoteAddr
    GeoIp.ip2rangeCity(ra) map { _ipGeoLoc =>
      val res = new GeoSearchInfo {
        override def geoPoint: geo.GeoPoint = gl.geoPoint
        override def geoDistanceQuery = GeoDistanceQuery(
          center      = gl.geoPoint,
          distanceMin = None,
          distanceMax = GeoLocation.DISTANCE_DFLT
        )
        override def exactGeopoint = Some(gl.geoPoint)

        override def ipGeopoint: Option[geo.GeoPoint] = {
          _ipGeoLoc map { _.city.geoPoint }
        }
        override def cityName = _ipGeoLoc.map(_.city.cityName)
        override def countryIso2 = _ipGeoLoc.map(_.range.countryIso2)
        override def isLocalClient = false
      }
      Some(res)
    }
  }

  override def isExact: Boolean = true

  override def exactGeodata = Some(geoPoint)

  /** Список уровней для детектирования в порядке употребления. Тут они выстраиваются с учётом точности. */
  override lazy val nodeDetectLevels: Seq[NodeGeoLevel] = {
    // Порядок ngl-значений должен быть такой: здание, район, город.
    val s0 = NodeGeoLevels.valuesNgl
      .iterator
    accuracyMeters
      .fold( s0 ) { am =>
        val amInt = am.toInt
        s0.filter { ngl =>
          ngl.accuracyMetersMax.isEmpty || ngl.accuracyMetersMax.exists(amInt <= _)
        }
      }
      .toSeq
  }

  override def asGeoLocation: Option[GeoLocation] = Some(this)
}
