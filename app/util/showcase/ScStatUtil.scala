package util.showcase

import models.im.DevScreenT
import models.{GeoSearchInfo, AdSearch}
import util.PlayMacroLogsImpl
import util.acl.AbstractRequestWithPwOpt
import util.event.SiowebNotifier.Implicts.sn
import play.api.http.HeaderNames.USER_AGENT

import io.suggest.util.UuidUtil
import models.stat.{ScStatActions, ScStatAction}
import net.sf.uadetector.service.UADetectorServiceFactory
import org.joda.time.DateTime
import util._
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

  def adSearchOpt: Option[AdSearch]

  def statAction: ScStatAction

  lazy val uaOpt = {
    request
      .headers
      .get(USER_AGENT)
      .map(_.trim)
      .filter(!_.isEmpty)
  }

  lazy val agent = uaOpt.flatMap { ua =>
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

  def forceFirstMadIds: Seq[String] = adSearchOpt.fold(Seq.empty[String])(_.forceFirstIds)

  def clickedAdIds = {
    if (withHeadAd && forceFirstMadIds.nonEmpty) {
      forceFirstMadIds
        .find { madIds.contains }
        .toSeq
    } else {
      Nil
    }
  }

  def clUidOpt = StatUtil.getFromRequest
    .map { UuidUtil.uuidToBase64 }

  val now = DateTime.now()

  def personId = request.pwOpt.map(_.personId)

  def adsCount = madIds.size

  def agentOs = agent.flatMap { _agent => Option(_agent.getOperatingSystem) }

  lazy val onNodeIdOpt: Option[String] = {
    adSearchOpt.flatMap { a =>
      a.receiverIds
        .headOption
        // Если задано много ресиверов, то не ясно, где именно оно было отражено.
        .filter { _ => a.receiverIds.size == 1 }
    }
  }

  def scSinkOpt: Option[AdnSink] = None

  def screenOpt: Option[DevScreenT] = adSearchOpt.flatMap(_.screen)

  def adnNodeOptFut = MAdnNodeCache.maybeGetByIdCached(onNodeIdOpt)

  def reqPath: Option[String] = Some(request.uri)

  def saveStats: Future[_] = {
    val screenOpt = this.screenOpt
    val agentOs = this.agentOs
    gsiFut flatMap { gsiOpt =>
      adnNodeOptFut flatMap { adnNodeOpt =>
        val adStat = new MAdStat(
          clientAddr  = request.remoteAddress,
          action      = statAction.toString(),
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
          clLocAccur  = adSearchOpt
            .flatMap { a => a.geo.asGeoLocation.flatMap(_.accuracyMeters).map(_.toInt) },
          isLocalCl   = request
            .isSuperuser || gsiOpt.fold(false)(_.isLocalClient),
          clOSFamily  = agentOs
            .flatMap { os => Option(os.getFamilyName) },
          clAgent     = agent
            .flatMap { _agent => Option(_agent.getName) },
          clDevice    = agent
            .flatMap { _agent => Option(_agent.getDeviceCategory) }
            .flatMap { dc => Option(dc.getName) },
          clickedAdIds = clickedAdIds,
          generation  = adSearchOpt.flatMap(_.generation),
          clOsVsn     = agentOs
            .flatMap { os => Option(os.getVersionNumber) }
            .flatMap { vsn => Option(vsn.getMajor) }
            .filter(!_.isEmpty),
          clUid       = clUidOpt,
          scrOrient   = screenOpt
            .map(_.orientation.name),
          scrResChoosen = screenOpt
            .flatMap(_.maybeBasicScreenSize)
            .map(_.toString()),
          pxRatioChoosen = screenOpt
            .map(_.pixelRatio.pixelRatio),
          viewportDecl = screenOpt
            .map(_.toString),
          reqUri = reqPath,
          scSink = scSinkOpt
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
 * @param request Данные запроса.
 */
case class ScTilesStatUtil(
  adSearch: AdSearch,
  madIds: Seq[String],
  gsiFut: Future[Option[GeoSearchInfo]]
)(implicit val request: AbstractRequestWithPwOpt[_])
  extends ScStatUtilT
{
  override def statAction = ScStatActions.Tiles
  override val adSearchOpt = Some(adSearch)
}


/**
 * Записывалка статистики для раскрытых рекламных карточек.
 * @param adSearch Запрошенный поиск рекламных карточек.
 * @param madIds ids возвращаемых рекламных карточек.
 * @param withHeadAd Испрользуется ли заглавная рекламная карточка?
 * @param request Данные запроса.
 */
case class ScFocusedAdsStatUtil(
  adSearch: AdSearch,
  madIds: Seq[String],
  override val withHeadAd: Boolean
)(
  implicit val request: AbstractRequestWithPwOpt[_]
)
  extends ScStatUtilT
{
  override def gsiFut = GeoIp.geoSearchInfoOpt
  override val adSearchOpt = Some(adSearch)
  override def statAction = ScStatActions.Opened
}


/** Эта статистика касается указанного узла. */
trait ScStatNode extends ScStatUtilT {
  val nodeOpt: Option[MAdnNode]
  override lazy val onNodeIdOpt = nodeOpt.flatMap(_.id)
  override def adnNodeOptFut = Future successful nodeOpt
}


/** Указываем, что тут рекламных карточек нет и не должно быть. */
trait ScStatNoAds extends ScStatUtilT {
  override def madIds: Seq[String] = Seq.empty
  override def adSearchOpt: Option[AdSearch] = None
}


/**
 * Записывалка статистики по обращению к разным showcase/index. Такая статистика позволяет отследить
 * перемещение юзера по узлам.
 * @param scSinkOpt sink выдачи, если известен.
 * @param gsiFut Асинхронный поиск геоданых по текущему запросу.
 * @param screenOpt данные по экрану.
 * @param request Текущий HTTP-реквест.
 */
case class ScIndexStatUtil(
  override val scSinkOpt: Option[AdnSink],
  gsiFut: Future[Option[GeoSearchInfo]],
  override val screenOpt: Option[DevScreenT],
  nodeOpt: Option[MAdnNode]
)(
  implicit val request: AbstractRequestWithPwOpt[_]
)
  extends ScStatUtilT with ScStatNode with ScStatNoAds
{
  override def statAction = ScStatActions.Index
}


/**
 * Записывалка статистики для обращения к demoWebSite-производным.
 * @param scSink Запрашиваемый синк выдачи.
 * @param nodeOpt Узел, если есть.
 * @param request HTTP-реквест.
 */
case class ScSiteStat(
  scSink: AdnSink,
  nodeOpt: Option[MAdnNode] = None
)(
  implicit val request: AbstractRequestWithPwOpt[_]
)
  extends ScStatUtilT with ScStatNode with ScStatNoAds
{
  override def gsiFut = GeoIp.geoSearchInfoOpt
  override def statAction = ScStatActions.Site
  override def scSinkOpt = Some(scSink)
}


/**
 * Записывалка статистики для обращения к findNode() экшену.
 * @param args Данные запроса поиска узлов.
 * @param gsiFut Асинхронные геоданные запроса.
 * @param request HTTP-Реквест.
 */
case class ScNodeListingStat(
  args: SimpleNodesSearchArgs,
  gsiFut: Future[Option[GeoSearchInfo]]
)(
  implicit val request: AbstractRequestWithPwOpt[_]
)
  extends ScStatUtilT with ScStatNoAds
{
  override def statAction = ScStatActions.Nodes
  override lazy val onNodeIdOpt: Option[String] = args.currAdnId
}

