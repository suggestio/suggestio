package io.suggest.loc.geo.ipgeobase

import com.google.inject.{Inject, Singleton}
import io.suggest.model.es.IEsModelDiVal
import io.suggest.model.geo.{GeoPoint, IGeoFindIp, IGeoFindIpResult}

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
      ipRanges  <- mIpRanges.findForIp(ip)
      cityEsIds = ipRanges.iterator
        .flatMap(_.cityId)
        .map(MCity.cityId2esId)
        .toSet
      mcities   <- mCities.multiGet(cityEsIds)
    } yield {
      for {
        mcity   <- mcities.headOption
        mrange  <- ipRanges.find(_.cityId == mcity.cityId)
      } yield {
        MGeoFindIpResult(mcity, mrange)
      }
    }
  }

}


/** Реализация модели результата работы [[IpgbUtil]].findId(). */
case class MGeoFindIpResult(city: MCity, range: MIpRange) extends IGeoFindIpResult {

  override def center: GeoPoint = {
    city.center
  }

  override def cityName: Option[String] = {
    Some( city.cityName )
  }

  override def countryIso2: Option[String] = {
    Some( range.countryIso2 )
  }

  override def accuracyMetersOpt: Option[Int] = {
    // TODO Нужно ли что-нибудь тут задать? Константу какую-либо например?
    None
  }

}
