package models

import io.suggest.geo._
import io.suggest.geo.{GeoDistanceQuery, IGeoFindIpResult}
import io.suggest.model.play.qsb.QueryStringBindableImpl
import models.req.ExtReqHdr
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.elasticsearch.common.unit.DistanceUnit
import play.api.http.HeaderNames
import play.api.mvc.QueryStringBindable
import play.api.Play.{configuration, current}
import util.geo.GeoIpUtil
import util.{PlayLazyMacroLogsImpl, PlayMacroLogsImpl}

import scala.concurrent.Future
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.14 10:31
 * Description: Режимы геолокации и утиль для них.
 */

@deprecated("Use MGeoLoc.geo instead", "2016.sep.16")
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
      case lat ~ lon =>
        MGeoPoint(lat = lat, lon = lon)
    }
    (latLonP ~ accurOptP) ^^ {
      case gp ~ accurOpt =>
        GeoLocation(gp, accuracyMeters = accurOpt)
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
    maybeApply(raw)
      .getOrElse(GeoNone)
  }

  /** Биндер для набега на GeoMode, сериализованный в qs. */
  implicit def geoModeQsb(implicit strOptB: QueryStringBindable[Option[String]]): QueryStringBindable[GeoMode] = {
    new QueryStringBindableImpl[GeoMode] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, GeoMode]] = {
        for (maybeGeo <- strOptB.bind(key, params)) yield {
          for (geo <- maybeGeo.right) yield {
            apply(geo)
          }
        }
      }

      override def unbind(key: String, value: GeoMode): String = {
        strOptB.unbind(key, value.toQsStringOpt)
      }
    }
  }

}


/** Интерфейс для режимов геопоиска. */
@deprecated("Use MGeoLoc.geo instead", "2016.sep.16")
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
  def geoSearchInfoOpt(implicit request: ExtReqHdr): Future[Option[GeoSearchInfo]]

  def exactGeodata: Option[MGeoPoint]

  /** Уровни, по которым надо искать. */
  def nodeDetectLevels: Seq[NodeGeoLevel]

  def asGeoLocation: Option[GeoLocation] = None

  def isExact: Boolean
}


@deprecated("Use utils.geo and others instead", "2016.sep.16")
trait GeoSearchInfo {
  /** Географическая точка, заданная координатами и описывающая клиента. */
  def geoPoint: MGeoPoint
  /** Сборка запроса для геопоиска относительно точки.. */
  def geoDistanceQuery: GeoDistanceQuery
  /** Название города. */
  def cityName: Option[String]
  /** Двухбуквенный код страны. */
  def countryIso2: Option[String]
  /** Точная геолокация клиента, если есть. */
  def exactGeopoint: Option[MGeoPoint]
  /** Координаты точки, которая набегает */
  def ipGeopoint: Option[MGeoPoint]
  /** Является ли браузер клиента частью cbca? */
  def isLocalClient: Boolean
}


/** Геолокация НЕ включена. */
@deprecated("Use MGeoLoc.geo instead", "2016.sep.16")
case object GeoNone extends GeoMode {
  override def isWithGeo = false
  override def toQsStringOpt = None
  override def geoSearchInfoOpt(implicit request: ExtReqHdr): Future[Option[GeoSearchInfo]] = {
    Future.successful( None )
  }
  override def exactGeodata = None

  /** Отсутвие геолокации означает отсутсвие уровней оной. */
  override def nodeDetectLevels = Nil

  override def isExact: Boolean = false
}


/** Геолокация по ip. */
@deprecated("Use MGeoLoc.geo None instead", "2016.sep.16")
case object GeoIp extends GeoMode with PlayMacroLogsImpl {

  private val geoIpUtil  = current.injector.instanceOf[GeoIpUtil]

  import LOGGER._

  val CACHE_TTL_SECONDS = configuration.getInt("geo.ip.cache.ttl.seconds").getOrElse( 240 )

  val DISTANCE_KM_DFLT: Double = configuration.getDouble("geo.ip.distance.km.dflt").getOrElse( 50.0 )

  def DISTANCE_DFLT = Distance(DISTANCE_KM_DFLT, DistanceUnit.KILOMETERS)

  /** Выставлять флаг local client, если User-Agent содержит подстроку, подходящую под этот регэксп. */
  val LOCAL_CL_UA_RE = "(?i)(NCDN|ngenix)".r

  override def isWithGeo = true
  override def toQsStringOpt = Some(GeoConstants.GEO_MODE_IP)
  override def geoSearchInfoOpt(implicit request: ExtReqHdr): Future[Option[GeoSearchInfo]] = {
    // Запускаем небыстрый синхронный поиск в отдельном потоке.
    val ra = getRemoteAddr
    ip2rangeCity(ra).map { resultOpt =>
      resultOpt.map { result =>
        new GeoSearchInfo {
          private def ipGeoPoint = result.center
          override def geoPoint = ipGeoPoint
          override def geoDistanceQuery = {
            GeoDistanceQuery(
              center = ipGeoPoint,
              distanceMax = DISTANCE_DFLT
            )
          }
          override def cityName = result.cityName
          override def countryIso2 = result.countryIso2
          override def exactGeopoint = None
          override def ipGeopoint = Option(geoPoint)
          override lazy val isLocalClient = {
            ra == geoIpUtil.REPLACE_LOCALHOST_IP_WITH || {
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

  def getRemoteAddr(implicit request: ExtReqHdr): String = {
    geoIpUtil.fixRemoteAddr(request.remoteAddress).remoteAddr
  }

  /** Асинхронный поиск какого-то ip в базе ip-адресов.
    * @param ip строка ip-адреса. */
  def ip2rangeCity(ip: String): Future[Option[IGeoFindIpResult]] = {
    geoIpUtil.findIpCached(ip)
  }

  override def exactGeodata: Option[MGeoPoint] = None

  override def nodeDetectLevels = Seq(NodeGeoLevels.NGL_TOWN)
}


@deprecated("Use MGeoLoc.geo Some instead", "2016.sep.16")
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
@deprecated("Use MGeoLoc.geo Some instead", "2016.sep.16")
final case class GeoLocation(geoPoint: MGeoPoint, accuracyMeters: Option[Double] = None) extends GeoMode { gl =>

  override def isWithGeo = true
  override def toQsStringOpt = Some(geoPoint.toQsStr)

  override def geoSearchInfoOpt(implicit request: ExtReqHdr): Future[Option[GeoSearchInfo]] = {
    val ra = GeoIp.getRemoteAddr
    for ( _ipGeoLoc <- GeoIp.ip2rangeCity(ra)) yield {
      val res = new GeoSearchInfo {
        override def geoPoint: MGeoPoint = gl.geoPoint
        override def geoDistanceQuery = GeoDistanceQuery(
          center      = gl.geoPoint,
          distanceMax = GeoLocation.DISTANCE_DFLT
        )
        override def exactGeopoint = Some(gl.geoPoint)

        override def ipGeopoint: Option[MGeoPoint] = {
          for (l <- _ipGeoLoc) yield {
            l.center
          }
        }
        override def cityName = _ipGeoLoc.flatMap(_.cityName)
        override def countryIso2 = _ipGeoLoc.flatMap(_.countryIso2)
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
