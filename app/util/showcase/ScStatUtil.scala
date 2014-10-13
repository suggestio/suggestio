package util.showcase

import io.suggest.ym.model.ad.AdsSearchArgsT
import models.{GeoSearchInfo, AdSearch}
import util.PlayMacroLogsImpl
import util.acl.AbstractRequestWithPwOpt
import util.event.SiowebNotifier.Implicts.sn
import play.api.http.HeaderNames.USER_AGENT

import io.suggest.model.OptStrId
import io.suggest.util.UuidUtil
import models.stat.{ScStatActions, ScStatAction}
import net.sf.uadetector.service.UADetectorServiceFactory
import org.joda.time.DateTime
import util._
import util.acl._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client
import util.stat.StatUtil
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.14 14:37
 * Description: Утиль для построения сборщиков статистики по разным экшенам SM-выдачи. + Сами сборщики статистики.
 * Этот велосипед возник из-за необъятности модели MAdStat и необходимости заполнять её немного по-разному
 * в разных случаях ситуациях, при этом с минимальной дубликацией кода и легкой расширяемостью оного.
 */
trait ScStatUtilT extends PlayMacroLogsImpl {

  import LOGGER._

  implicit def request: AbstractRequestWithPwOpt[_]
  def gsiFut: Future[Option[GeoSearchInfo]]
  def madIds: Seq[String]

  def adSearch: Option[AdsSearchArgsT]

  def statAction: ScStatAction

  val uaOpt = {
    request
      .headers
      .get(USER_AGENT)
      .filter(!_.isEmpty)
  }

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

  def withHeadAd: Boolean = false

  def forceFirstMadIds: Seq[String] = adSearch.fold(Seq.empty[String])(_.forceFirstIds)

  val clickedAdIds = {
    if (withHeadAd && forceFirstMadIds.nonEmpty) {
      forceFirstMadIds
        .find { madIds.contains }
        .toSeq
    } else {
      Nil
    }
  }

  val clUidOpt = StatUtil.getFromRequest
    .map { UuidUtil.uuidToBase64 }

  val now = DateTime.now()

  val personId = request.pwOpt.map(_.personId)

  val adsCount = madIds.size

  val agentOs = agent.flatMap { _agent => Option(_agent.getOperatingSystem) }

  val onNodeIdOpt: Option[String] = {
    adSearch.flatMap { a =>
      a.receiverIds
        .headOption
        // Если задано много ресиверов, то не ясно, где именно оно было отражено.
        .filter { _ => a.receiverIds.size == 1 }
    }
  }

  val adnNodeOptFut = MAdnNodeCache.maybeGetByIdCached(onNodeIdOpt)

  def saveStats(a: AdSearch) : Future[_] = {
    gsiFut flatMap { gsiOpt =>
      adnNodeOptFut flatMap { adnNodeOpt =>
        val adStat = new MAdStat(
          clientAddr  = request.remoteAddress,
          action      = statAction.toString,
          adIds       = madIds,
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
          clLocAccur  = a.geo.asGeoLocation.flatMap(_.accuracyMeters).map(_.toInt),
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
  }

}


/**
 * Записывалка статистики для плитки выдачи.
 * @param adSearch Запрошеный поиск рекламных карточек.
 * @param madIds id возвращаемых рекламных карточек.
 * @param gsiFut Данные о геолокации обычно доступны на уровне выдачи.
 * @param request
 */
case class ScTilesStatUtil(
  adSearch: AdSearch,
  madIds: Seq[String],
  gsiFut: Future[Option[GeoSearchInfo]]
)(implicit val request: AbstractRequestWithPwOpt[_])
  extends ScStatUtilT
{
  override def statAction = ScStatActions.Tiles
}


