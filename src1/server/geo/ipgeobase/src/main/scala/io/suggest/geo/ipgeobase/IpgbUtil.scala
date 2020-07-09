package io.suggest.geo.ipgeobase

import javax.inject.{Inject, Singleton}
import io.suggest.es.model.EsModel
import io.suggest.geo.{IGeoFindIp, IGeoFindIpResult, MGeoPoint}
import io.suggest.util.logs.MacroLogsImpl
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.16 13:09
  * Description: Над-утиль для подсистемы ipgeobase.
  * Появилась в ходе выноса ipgb-логики на уровень этого модуля из устаревшей GeoMode.
  */
@Singleton
final class IpgbUtil @Inject() (
                                 injector: Injector,
                               )
  extends IGeoFindIp
  with MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mIpRangesModel = injector.instanceOf[MIpRangesModel]
  private lazy val mCities = injector.instanceOf[MCities]
  private lazy val mIpRanges = injector.instanceOf[MIpRanges]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  override type FindIpRes_t = MGeoFindIpResult

  /**
    * API для поиска ip-адреса без какого-либо знания внутренностей подсистемы ipgeobase.
    *
    * @param ip Искомый ip-адрес.
    * @return Фьючерс с опциональным результатом.
    */
  override def findIp(ip: String): Future[Option[MGeoFindIpResult]] = {
    import esModel.api._
    import mIpRangesModel.api._

    for {
      // Найти диапазоны ip-адресов
      ipRanges  <- mIpRanges.findForIp(ip)

      // Выявить id городов, связанных с найденными диапазонами.
      cityEsIds = (for {
        ipRange <- ipRanges.iterator
        cityId  <- ipRange.cityId
      } yield
        MCity.cityId2esId( cityId )
      )
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
final case class MGeoFindIpResult(
                                   city: MCity,
                                   range: MIpRange
                                 )
  extends IGeoFindIpResult
{

  override def center: MGeoPoint =
    city.center

  override def cityName: Option[String] =
    Some( city.cityName )

  override def countryIso2: Option[String] =
    Some( range.countryIso2 )

  override def accuracyMetersOpt: Option[Int] =
    // TODO Нужно ли что-нибудь тут задать? 50км например?
    Some( 20000 )

}
