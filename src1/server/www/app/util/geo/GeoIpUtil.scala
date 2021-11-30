package util.geo

import javax.inject.Inject
import io.suggest.geo.{IGeoFindIp, IGeoFindIpResult, MGeoLoc}
import io.suggest.geo.ipgeobase.IpgbUtil
import io.suggest.playx.CacheApiUtil
import io.suggest.util.logs.MacroLogsImpl
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.16 12:05
  * Description: Над-модельная утиль для геолокации по IP.
  * Модуль создан, чтобы прятать в себе все используемые geoip-подсистемы.
  *
  * Изначально поддерживалась только IPGeoBase.
  */
class GeoIpUtil @Inject() (
                            injector    : Injector,
                          )
  extends IGeoFindIp
  with MacroLogsImpl
{

  override type FindIpRes_t = IGeoFindIpResult

  private lazy val ipgbUtil = injector.instanceOf[IpgbUtil]
  private lazy val cacheApiUtil = injector.instanceOf[CacheApiUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  /** Сколько секунд кешировать результат работы findIdCached()? */
  def CACHE_TTL_SEC = 10

  /**
    * Поиск геоданных для IP в geoip-подсистемах.
    *
    * @param ip ip-адрес типа "122.133.144.155".
    * @return Фьючерс с опциональным результатом геолокации.
    */
  override def findIp(ip: String): Future[Option[IGeoFindIpResult]] = {
    val resFut = ipgbUtil.findIp(ip)

    // Логгировать результат, если трассировка активна.
    if (LOGGER.underlying.isTraceEnabled()) {
      val startedAtMs = System.currentTimeMillis()

      for (r <- resFut)
        LOGGER.trace(s"findIp($ip) => $r ;; Took ${System.currentTimeMillis() - startedAtMs} ms.")
    }

    // Вернуть исходный фьючерс.
    resFut
  }

  /**
    * Кэишруемый аналог findIp().
    *
    * @param ip ip-адрес типа "122.133.144.155".
    * @return Фьючерс с опциональным результатом геолокации.
    */
  def findIpCached(ip: String): Future[Option[IGeoFindIpResult]] = {
    cacheApiUtil.getOrElseFut(ip + ".gIpF", expiration = CACHE_TTL_SEC.seconds) {
      findIp(ip)
    }
  }


  /** Костыль: приведение IGeoFindIpResult к MGeoLoc. */
  def geoIpRes2geoLocOptFut(geoIpResOptFut: Future[Option[IGeoFindIpResult]]): Future[Option[MGeoLoc]] = {
    for (geoIpOpt <- geoIpResOptFut) yield {
      for (geoIp <- geoIpOpt) yield {
        geoIp.toGeoLoc
      }
    }
  }

  /**
    * Попытаться заполнить возможно-пустующие данные модели геолокации по данным из geoip.
    * @param geoLocOpt0 Исходные опциональные данные геолокации.
    * @param geoIpLocOptFut Функция запуска geoip-геолокации.
    * @return Фьючерс с опциональным результатом MGeoLoc, как правило Some().
    */
  def geoLocOrFromIp(geoLocOpt0: Seq[MGeoLoc])
                    (geoIpLocOptFut: => Future[Seq[MGeoLoc]]): Future[Seq[MGeoLoc]] = {
    if (geoLocOpt0.isEmpty) {
      // Подавить и залоггировать возможные проблемы.
      geoIpLocOptFut.recover { case ex: Throwable =>
        LOGGER.warn(s"geoLocOrFromIp($geoLocOpt0): failed to geoIP", ex)
        Nil
      }
    } else {
      Future.successful( geoLocOpt0 )
    }
  }

}
