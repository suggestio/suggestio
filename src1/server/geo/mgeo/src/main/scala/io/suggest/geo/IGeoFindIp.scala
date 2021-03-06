package io.suggest.geo

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.16 13:17
  * Description: Интерфейс для опциональных результатов findIp().
  */
trait IGeoFindIp {

  /** Тип положительного результата findIp(). */
  type FindIpRes_t <: IGeoFindIpResult

  /** API для простого поиска результатов для ip в рамках какой-либо geoip-подсистемы.
    *
    * @param ip ip-адрес типа "122.133.144.155".
    * @return Фьючерс с опциональным результатом геолокации.
    */
  def findIp(ip: String): Future[Option[FindIpRes_t]]

}


/** Интерфейс результата [[IGeoFindIp]].find(). */
trait IGeoFindIpResult {

  /** Точка на карте. */
  def center: MGeoPoint

  /** Имя города, если известно. */
  def cityName: Option[String]

  /** Код страны, которой делегирован ip-адрес. */
  def countryIso2: Option[String]

  /** Точность ip-геолокации в метрах. */
  def accuracyMetersOpt: Option[Int]


  def toGeoLoc: MGeoLoc =
    MGeoLoc( center, Some(MGeoLocSources.GeoIP), accuracyMetersOpt.map(_.toDouble) )

}
