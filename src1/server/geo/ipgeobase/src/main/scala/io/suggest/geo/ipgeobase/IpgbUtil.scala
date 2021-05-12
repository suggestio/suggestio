package io.suggest.geo.ipgeobase

import javax.inject.{Inject, Singleton}
import io.suggest.es.model.EsModel
import io.suggest.geo.{IGeoFindIp, IGeoFindIpResult, MGeoPoint}
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
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
  private lazy val mIpgbItemsModel = injector.instanceOf[MIpgbItemsModel]
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
    import mIpgbItemsModel.api._

    val mIpgbItems = MIpgbItems.CURRENT
    for {
      // Найти диапазоны ip-адресов
      ipRanges  <- mIpgbItems.findForIp(ip)

      // Выявить id городов, связанных с найденными диапазонами.
      cityEsIds = (for {
        ipRange <- ipRanges.iterator
        cityId  <- ipRange.payload.cityId
      } yield
        MIpgbItem.cityId2esId( cityId )
      )
        .toSet

      // Получить города по city ids.
      mcities   <- mIpgbItems.multiGet(cityEsIds)

    } yield {
      lazy val logPrefix = s"findId($ip):"

      if (mcities.lengthIs > 1)
        LOGGER.warn(s"$logPrefix Too many ${mcities.length} IPGB GeoLoc results found, but 0 or 1 expected. Some of the results will be dropped.\n ALL results are:\n ${mcities.mkString("\n ")}")

      // Собрать опциональный результат, залоггировать, вернуть.
      val r = (for {
        mcity <- mcities.iterator
        if {
          val r = (mcity.payload.itemType ==* MIpgbItemTypes.City)
          if (!r) LOGGER.warn(s"$logPrefix Expected city, but iprange received: $mcity")
          r
        }
        mrange  <- ipRanges.find { ipRange =>
          ipRange.payload.cityId ==* mcity.payload.cityId &&
          ipRange.payload.cityId.nonEmpty
        }
        center <- {
          val centerOpt = mcity.payload.center
          if (centerOpt.isEmpty) LOGGER.error(s"$logPrefix City#${mcity.idOrNull} found, but center geoPoint missing or invalid:\n $mcity")
          centerOpt
        }
      } yield {
        MGeoFindIpResult(center, mcity, mrange)
      })
        .nextOption()

      LOGGER.trace(s"$logPrefix\n IP Ranges:\t${ipRanges.mkString(", ")}\n Cities:\t${mcities.mkString(", ")}\n Result:\t$r")
      r
    }
  }

}


/** Реализация модели результата работы [[IpgbUtil]].findId(). */
final case class MGeoFindIpResult(
                                   override val center: MGeoPoint,
                                   city: MIpgbItem,
                                   range: MIpgbItem,
                                 )
  extends IGeoFindIpResult
{

  override def cityName: Option[String] =
    city.payload.cityName

  override def countryIso2: Option[String] =
    range.payload.countryIso2

  override def accuracyMetersOpt: Option[Int] =
    // TODO Нужно ли что-нибудь тут задать? 50км например?
    Some( 20000 )

}
