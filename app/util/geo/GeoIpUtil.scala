package util.geo

import java.net.InetAddress

import com.google.inject.{Inject, Singleton}
import io.suggest.loc.geo.ipgeobase.IpgbUtil
import io.suggest.model.geo.{IGeoFindIp, IGeoFindIpResult}
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl

import scala.concurrent.Future
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
@Singleton
class GeoIpUtil @Inject() (
  ipgbUtil    : IpgbUtil,
  mCommonDi   : ICommonDi
)
  extends IGeoFindIp
  with PlayMacroLogsImpl
{

  override type FindIpRes_t = IGeoFindIpResult

  import mCommonDi._

  /** Сколько секунд кешировать результат работы findIdCached()? */
  def CACHE_TTL_SEC = 10

  /** На какой ip-адрес заменять локалхост? */
  def REPLACE_LOCALHOST_IP_WITH = "213.108.35.158"


  /**
    * Поиск геоданных для IP в geoip-подсистемах.
    *
    * @param ip ip-адрес типа "122.133.144.155".
    * @return Фьючерс с опциональным результатом геолокации.
    */
  override def findIp(ip: String): Future[Option[FindIpRes_t]] = {
    val resFut = ipgbUtil.findIp(ip)

    // Логгировать результат, если трассировка активна.
    if (LOGGER.underlying.isTraceEnabled()) {
      val startedAtMs = System.currentTimeMillis()
      resFut.onSuccess { case r =>
        LOGGER.trace(s"findIp($ip) => $r ;; Took ${System.currentTimeMillis() - startedAtMs} ms.")
      }
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
  def findIpCached(ip: String): Future[Option[FindIpRes_t]] = {
    cacheApiUtil.getOrElseFut(ip + ".gIpF", expiration = CACHE_TTL_SEC.seconds) {
      findIp(ip)
    }
  }


  /**
    * Если это локальный адрес, то нужно его подменить на адрес офиса cbca. Это нужно для облегчения отладки.
    * Обёрнуто в try чтобы подавлять сюрпризы содержимого remoteAddress.
    *
    * @param remoteAddr0 Сырое значение адреса request.remoteAddress, желательно из ExtReqHdr.
    * @return Строка с поправленным для поиска по geoip адресом.
    */
  def fixRemoteAddr(remoteAddr0: String): String = {
    lazy val logPrefix = s"getRemoteAddr($remoteAddr0):"
    try {
      val inetAddr = InetAddress.getByName(remoteAddr0)
      if (inetAddr.isAnyLocalAddress || inetAddr.isSiteLocalAddress || inetAddr.isLoopbackAddress) {
        val ra1 = REPLACE_LOCALHOST_IP_WITH
        LOGGER.trace(s"$logPrefix Local ip detected: $remoteAddr0. Rewriting ip with $ra1")
        ra1
      } else {
        remoteAddr0
      }
    } catch {
      case ex: Exception =>
        LOGGER.error(s"$logPrefix Failed to parse remote address", ex)
        remoteAddr0
    }
  }

}


/** Интерфейс для DI-поля с инстансом [[GeoIpUtil]]. */
trait IGeoIpUtilDi {
  def geoIpUtil: GeoIpUtil
}
