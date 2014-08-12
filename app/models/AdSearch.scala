package models

import java.net.InetAddress

import io.suggest.ym.model.common.{Distance, GeoDistanceQuery}
import org.elasticsearch.common.unit.DistanceUnit
import play.api.cache.Cache
import play.api.db.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{RequestHeader, QueryStringBindable}
import play.api.Play.{current, configuration}
import io.suggest.ym.model.ad.AdsSearchArgsT
import util.PlayMacroLogsImpl
import util.qsb.QsbUtil._
import scala.concurrent.{Future, future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 16:05
 * Description: Модель представления поискового запроса.
 */

object AdSearch {

  /** Максимальное число результатов в ответе на запрос (макс. результатов на странице). */
  val MAX_RESULTS_PER_RESPONSE = configuration.getInt("market.search.ad.results.max") getOrElse 50

  /** Кол-во результатов на страницу по дефолту. */
  val MAX_RESULTS_DFLT = configuration.getInt("market.search.ad.results.count.dflt") getOrElse 20

  /** Макс.кол-во сдвигов в страницах. */
  val MAX_PAGE_OFFSET = configuration.getInt("market.search.ad.results.offset.max") getOrElse 20

  private implicit def eitherOpt2list[T](e: Either[_, Option[T]]): List[T] = {
    e match {
      case Left(_)  => Nil
      case Right(b) => b.toList
    }
  }

  implicit def queryStringBinder(implicit strOptBinder: QueryStringBindable[Option[String]], intOptBinder: QueryStringBindable[Option[Int]], longOptBinder: QueryStringBindable[Option[Long]]) = {
    new QueryStringBindable[AdSearch] {
      def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AdSearch]] = {
        for {
          maybeProdIdOpt <- strOptBinder.bind(key + ".shopId", params)
          maybeCatIdOpt  <- strOptBinder.bind(key + ".catId", params)
          maybeLevelOpt  <- strOptBinder.bind(key + ".level", params)
          maybeQOpt      <- strOptBinder.bind(key + ".q", params)
          maybeSizeOpt   <- intOptBinder.bind(key + ".size", params)
          maybeOffsetOpt <- intOptBinder.bind(key + ".offset", params)
          maybeRcvrIdOpt <- strOptBinder.bind(key + ".rcvr", params)
          maybeFirstId   <- strOptBinder.bind(key + ".firstAdId", params)
          maybeGen       <- longOptBinder.bind(key + ".gen", params)
          maybeGeo       <- strOptBinder.bind(key + ".geo", params)

        } yield {
          Right(
            AdSearch(
              receiverIds = maybeRcvrIdOpt,
              producerIds = maybeProdIdOpt,
              catIds      = maybeCatIdOpt,
              levels      = eitherOpt2list(maybeLevelOpt).flatMap(AdShowLevels.maybeWithName),
              qOpt        = maybeQOpt,
              maxResultsOpt = eitherOpt2option(maybeSizeOpt) map { size =>
                Math.max(4,  Math.min(size, MAX_RESULTS_PER_RESPONSE))
              },
              offsetOpt   = eitherOpt2option(maybeOffsetOpt) map { offset =>
                Math.max(0,  Math.min(offset,  MAX_PAGE_OFFSET * maybeSizeOpt.getOrElse(10)))
              },
              forceFirstIds = maybeFirstId,
              generation  = maybeGen,
              geo         = AdsGeoMode(maybeGeo)
            )
          )
        }
      }

      def unbind(key: String, value: AdSearch): String = {
        List(
          strOptBinder.unbind(key + ".rcvr", value.receiverIds.headOption),   // TODO Разбиндивать на весь список receivers сразу надо
          strOptBinder.unbind(key + ".shopId", value.producerIds.headOption), // TODO Разбиндивать на весь список producers сразу надо.
          strOptBinder.unbind(key + ".catId", value.catIds.headOption),       // TODO Разбиндивать на весь список catIds надо бы
          strOptBinder.unbind(key + ".level", value.levels.headOption.map(_.toString)),
          strOptBinder.unbind(key + ".q", value.qOpt),
          intOptBinder.unbind(key + ".size", value.maxResultsOpt),
          intOptBinder.unbind(key + ".offset", value.offsetOpt),
          strOptBinder.unbind(key + ".firstAdId", value.forceFirstIds.headOption),
          longOptBinder.unbind(key + ".gen", value.generation),
          strOptBinder.unbind(key + ".geo", value.geo.toQsStringOpt)
        ) .filter(!_.isEmpty)
          .mkString("&")
      }
    }
  }

}

case class AdSearch(
  receiverIds : List[String] = Nil,
  producerIds : List[String] = Nil,
  catIds      : List[String] = Nil,
  levels      : List[AdShowLevel] = Nil,
  qOpt: Option[String] = None,
  maxResultsOpt: Option[Int] = None,
  offsetOpt: Option[Int] = None,
  forceFirstIds: List[String] = Nil,
  generation  : Option[Long] = None,
  withoutIds  : List[String] = Nil,
  geo         : AdsGeoMode = AdsGeoNone
) extends AdsSearchArgsT {

  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  def offset: Int = if (offsetOpt.isDefined) offsetOpt.get else 0

  /** Макс.кол-во результатов. */
  def maxResults: Int = maxResultsOpt getOrElse AdSearch.MAX_RESULTS_DFLT
}


// Режимы геолокации и утиль для них.
/** Статичекая утиль для поддержки параметра геолокации. */
object AdsGeoMode {

  /** Регэксп для извлечения координат из строки, переданной веб-мордой. */
  val LAT_LON_RE = """-?\d{1,3}\.\d{0,8},-?\d{1,3}\.\d{0,8}""".r

  /** Распарсить опциональное сырое значение qs-параметра a.geo=. */
  def apply(raw: Option[String]): AdsGeoMode = {
    raw.fold [AdsGeoMode] (AdsGeoNone) {
      case "ip"  => AdsGeoIp
      case LAT_LON_RE(latStr, lonStr) =>
        AdsGeoLocation(latStr.toDouble, lonStr.toDouble)
      case other => AdsGeoNone
    }
  }
}

/** Интерфейс для режимов геопоиска. */
sealed trait AdsGeoMode {
  /** Запрошен ли географический поиск? */
  def isWithGeo: Boolean

  /** Сериализовать назад в строку qs. */
  def toQsStringOpt: Option[String]

  /** Подготовить геоданные для поиска в ES. */
  def geoSearchInfo(implicit request: RequestHeader): Future[Option[GeoDistanceQuery]]
}

/** Геолокация НЕ включена. */
case object AdsGeoNone extends AdsGeoMode {
  override def isWithGeo = false
  override def toQsStringOpt = None
  override def geoSearchInfo(implicit request: RequestHeader) = Future successful None
}

/** Геолокация по ip. */
case object AdsGeoIp extends AdsGeoMode {

  val CACHE_TTL_SECONDS = configuration.getInt("ads.search.geo.ip.cache.ttl.seconds") getOrElse 240

  override def isWithGeo = true
  override def toQsStringOpt = Some("ip")
  override def geoSearchInfo(implicit request: RequestHeader): Future[Option[GeoDistanceQuery]] = {
    val ra = request.remoteAddress
    if (ra startsWith "127.") {
      Future successful None
    } else {
      future {
        val ck = ra + ".gipq"
        Cache.getOrElse(ck, CACHE_TTL_SECONDS) {
          DB.withConnection { implicit c =>
            IpGeoBaseRange.findForIp(InetAddress getByName ra)
              .headOption
              .flatMap { _.cityOpt }
              .map { city =>
                GeoDistanceQuery(
                  center = city.geoPoint,
                  distanceMin = None,
                  distanceMax = Distance(50, DistanceUnit.KILOMETERS)
                )
              }
          } // DB
        }   // Cache
      }     // future()
    }       // if localhost
  }         // def geoSearchInfo()

}

/** Геолокация с указанием географических координат. */
case class AdsGeoLocation(lat: Double, lon: Double) extends AdsGeoMode {
  override def isWithGeo = true
  override def toQsStringOpt = Some(s"$lat,$lon")
  override def geoSearchInfo(implicit request: RequestHeader): Future[Option[GeoDistanceQuery]] = {
    val result = GeoDistanceQuery(
      center      = GeoPoint(lat = lat, lon = lon),
      distanceMin = None,
      distanceMax = Distance(5, DistanceUnit.KILOMETERS)
    )
    Future successful Some(result)
  }
}

