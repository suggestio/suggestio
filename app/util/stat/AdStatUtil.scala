package util.stat

import io.suggest.model.OptStrId
import net.sf.uadetector.service.UADetectorServiceFactory
import org.joda.time.DateTime
import util._
import util.acl._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client
import scala.concurrent.Future
import play.api.Play.{current, configuration}
import controllers.Application.USER_AGENT
import scala.util.{Failure, Success}
import util.event.SiowebNotifier.Implicts.sn

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.08.14 13:32
 * Description: Утиль для сбора статистики по рекламной выдаче.
 */
object AdStatUtil extends PlayMacroLogsImpl {

  import LOGGER._

  /** Создать и сохранить статистику для указанных карточек. Сохранение идёт в фоне, остальное в текущем потоке.
    * @param a AdSearch
    * @param mads Список рекламных карточек.
    * @param statAction Какую пометку выставлять в поле stat action: клик или просмотр или ...
    * @param withHeadAd аналог параметра h у focusedAds(). Его наличие для статистики означает,
    *                   что в начале списка может быть карточка, по которой кликнули.
    * @param request Инстанс текущего реквеста.
    * @return Фьючерс для синхронизации. Сохранение идёт асинхронно.
    */
  def saveAdStats(a: AdSearch, mads: Seq[OptStrId], statAction: AdStatAction,
                  gsi: Option[Future[Option[GeoSearchInfo]]] = None, withHeadAd: Boolean = false)
                (implicit request: AbstractRequestWithPwOpt[_]): Future[_] = {
    // Отрендеренные рекламные карточки нужно учитывать через статистику просмотров.
    // Запускаем асинхронные задачи:
    val gsiFut = gsi getOrElse GeoIp.geoSearchInfoOpt
    val onNodeIdOpt = a.receiverIds
      .headOption
      .filter { _ => a.receiverIds.size == 1 }    // Если задано много ресиверов, то не ясно, где именно оно было отражено.
    val adnNodeOptFut = MAdnNodeCache.maybeGetByIdCached(onNodeIdOpt)
    // Синхронно парсим юзер-агент, затем заполняем данными новый экземпляр MAdStat.
    val uaOpt = request.headers.get(USER_AGENT).filter(!_.isEmpty)
    val agent = uaOpt.flatMap { ua =>
      // try-catch для самозащиты от возможных багов в православной либе uadetector.
      try {
        val uaParser = UADetectorServiceFactory.getResourceModuleParser
        Some(uaParser.parse(ua))
      } catch {
        case ex: Throwable =>
          warn("saveStats(): Unable to use UADetector for parsing UA: " + ua, ex)
          None
      }
    }
    val personId = request.pwOpt.map(_.personId)
    val ra = request.remoteAddress
    val now = DateTime.now()
    val adIds = mads.flatMap(_.id)
    val adsCount = adIds.size
    // Определяем карточку по которой кликнули: нужно проверсти онализ withHeadAd, списка карточек и списка id первых карточек.
    val clickedAdIds = if (withHeadAd && a.forceFirstIds.nonEmpty) {
      a.forceFirstIds
        .find { firstAdId =>
          mads.exists(_.id.exists(_ == firstAdId))
        }
        .toSeq
    } else {
      Nil
    }
    val clUidOpt = StatUtil.getFromRequest
      .map { StatUtil.uuidToBase64 }
    val resultFut = gsiFut flatMap { gsiOpt =>
      adnNodeOptFut flatMap { adnNodeOpt =>
        val agentOs = agent.flatMap { _agent => Option(_agent.getOperatingSystem) }
        val adStat = new MAdStat(
          clientAddr  = ra,
          action      = statAction,
          adIds       = adIds,
          adsRendered = adsCount,
          onNodeIdOpt = onNodeIdOpt,
          nodeName    = adnNodeOpt.map(_.meta.name),
          ua          = uaOpt,
          personId    = personId,
          timestamp   = now,
          clIpGeo     = gsiOpt.flatMap(_.ipGeopoint),
          clTown      = gsiOpt.flatMap(_.cityName),
          clGeoLoc    = gsiOpt.flatMap(_.exactGeopoint),
          clCountry   = gsiOpt.flatMap(_.countryIso2),
          isLocalCl   = request.isSuperuser || gsiOpt.fold(false)(_.isLocalClient),
          clOSFamily  = agentOs.flatMap { os => Option(os.getFamilyName) },
          clAgent     = agent.flatMap { _agent => Option(_agent.getName) },
          clDevice    = agent
            .flatMap { _agent => Option(_agent.getDeviceCategory) }
            .flatMap { dc => Option(dc.getName) },
          clickedAdIds = clickedAdIds,
          generation  = a.generation,
          clOsVsn     = agentOs
            .flatMap { os => Option(os.getVersionNumber) }
            .flatMap { vsn => Option(vsn.getMajor) }
            .filter(!_.isEmpty),
          clUid       = clUidOpt
        )
        //trace(s"Saving MAdStat with: clOsVsn=${adStat.clOsVsn} clUid=${adStat.clUid}")
        adStat.save
      }
    }
    // Вешаем логгирование результатов на запущенный реквест.
    resultFut onComplete {
      case Success(adStatId) =>
        trace(s"saveStats(): Saved successful: id = $adStatId for $adsCount ads.")
      case Failure(ex) =>
        error(s"saveStats(): Failed to save statistics for $adsCount ads", ex)
    }
    // Возвращаем фьючерс.
    resultFut
  }


}
