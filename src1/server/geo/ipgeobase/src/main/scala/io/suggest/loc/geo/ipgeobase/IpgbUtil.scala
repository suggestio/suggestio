package io.suggest.loc.geo.ipgeobase

import javax.inject.{Inject, Singleton}
import io.suggest.es.model.IEsModelDiVal
import io.suggest.geo.{IGeoFindIp, IGeoFindIpResult, MGeoPoint}
import io.suggest.util.logs.MacroLogsImpl

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.16 13:09
  * Description: Над-утиль для подсистемы ipgeobase.
  * Появилась в ходе выноса ipgb-логики на уровень этого модуля из устаревшей GeoMode.
  */
@Singleton
class IpgbUtil @Inject() (
  mCities     : MCities,
  mIpRanges   : MIpRanges,
  mCommonDi   : IEsModelDiVal
)
  extends IGeoFindIp
  with MacroLogsImpl
{

  import mCommonDi._


  override type FindIpRes_t = MGeoFindIpResult

  /**
    * API для поиска ip-адреса без какого-либо знания внутренностей подсистемы ipgeobase.
    *
    * @param ip Искомый ip-адрес.
    * @return Фьючерс с опциональным результатом.
    */
  override def findIp(ip: String): Future[Option[MGeoFindIpResult]] = {
    for {
      // Найти диапазоны ip-адресов
      ipRanges  <- mIpRanges.findForIp(ip)

      // Выявить id городов, связанных с найденными диапазонами.
      cityEsIds = ipRanges.iterator
        .flatMap(_.cityId)
        .map(MCity.cityId2esId)
        .toSet

      // Получить города по city ids.
      mcities   <- mCities.multiGet(cityEsIds)

    } yield {

      // Собрать опциональный результат, залоггировать, вернуть.
      val r = for {
        mcity   <- mcities.headOption
        mrange  <- ipRanges.find(_.cityId.contains( mcity.cityId ))
      } yield {
        MGeoFindIpResult(mcity, mrange)
      }

      LOGGER.trace(s"findId($ip):\n IP Ranges:\t${ipRanges.mkString(", ")}\n Cities:\t${mcities.mkString(", ")}\n Result:\t$r")
      r
    }
  }

}


/** Реализация модели результата работы [[IpgbUtil]].findId(). */
case class MGeoFindIpResult(city: MCity, range: MIpRange) extends IGeoFindIpResult {

  override def center: MGeoPoint = {
    city.center
  }

  override def cityName: Option[String] = {
    Some( city.cityName )
  }

  override def countryIso2: Option[String] = {
    Some( range.countryIso2 )
  }

  override def accuracyMetersOpt: Option[Int] = {
    // TODO Нужно ли что-нибудь тут задать? 50км например?
    Some( 20000 )
  }

}
