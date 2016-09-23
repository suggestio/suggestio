package util.showcase

import com.google.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.model.geo.IGeoFindIpResult
import io.suggest.util.UuidUtil
import io.suggest.ym.model.stat.MAdStat
import io.suggest.stat.m._
import io.suggest.stat.saver.PlayStatSaver
import models.im.DevScreen
import models.mctx.Context
import models.mgeo.MLocEnv
import models.mproj.ICommonDi
import models.msc.MScNodeSearchArgs
import models.req.{IReqHdr, ISioUser}
import models.stat.{ScStatAction, ScStatActions}
import models.{AdSearch, GeoSearchInfo, _}
import net.sf.uadetector.service.UADetectorServiceFactory
import org.joda.time.DateTime
import play.api.http.HeaderNames.USER_AGENT
import util.PlayMacroLogsImpl
import util.geo.GeoIpUtil
import util.stat.StatCookiesUtil

import scala.concurrent.duration._
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.14 14:37
 * Description: Утиль для построения сборщиков статистики по разным экшенам SM-выдачи. + Сами сборщики статистики.
 * Этот велосипед возник из-за необъятности модели MAdStat и необходимости заполнять её немного по-разному
 * в разных случаях ситуациях, при этом с минимальной дубликацией кода и легкой расширяемостью оного.
 */
class ScStatUtil @Inject() (
  statCookiesUtil         : StatCookiesUtil,
  scStatSaver             : ScStatSaver,
  playStatSaver           : PlayStatSaver,
  geoIpUtil               : GeoIpUtil,
  mCommonDi               : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._
  import LOGGER._

  /** Локальных клиентов нет смысла долго хранить. Время их хранения тут. */
  val LOCAL_STAT_TTL = {
    val d = configuration.getInt("sc.stat.local.ttl.days").getOrElse(7)
    d.days
  }

  /** Является ли указанная строка мусорной? */
  def isStrGarbage(str: String): Boolean = {
    (str == null) ||
      str.isEmpty ||
      str.equalsIgnoreCase("null") ||
      str.equalsIgnoreCase("undefined") ||
      str.equalsIgnoreCase("unknown")
  }

  def isStrUseful(str: String) = !isStrGarbage(str)


  /** common-утиль для сборки генераторов статистики. */
  abstract class StatT {

    implicit def request: IReqHdr

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

    def forceFirstMadIds: Seq[String] = adSearchOpt.fold(Seq.empty[String])(_.firstIds)

    def clickedAdIds = {
      if (withHeadAd && forceFirstMadIds.nonEmpty) {
        forceFirstMadIds
          .find { madIds.contains }
          .toSeq
      } else {
        Nil
      }
    }

    def clUidOpt = statCookiesUtil
      .getFromRequest
      .map { UuidUtil.uuidToBase64 }

    def agentOs = agent.flatMap { _agent =>
      Option(_agent.getOperatingSystem)
    }

    val now = DateTime.now()

    def personId = request.user.personIdOpt

    def adsCount = madIds.size

    lazy val onNodeIdOpt: Option[String] = {
      adSearchOpt.flatMap { a =>
        val rcvrs = {
          a.outEdges
            .iterator
            .filter { cr =>
              cr.containsPredicate(MPredicates.Receiver) && cr.nodeIds.nonEmpty
            }
            .toStream
        }
        // Если задано много ресиверов, то не ясно, где именно оно было отражено.
        if (rcvrs.size == 1) {
          Some(rcvrs.head.nodeIds.head)
        } else {
          None
        }
      }
    }

    def screenOpt: Option[DevScreen] = adSearchOpt.flatMap(_.screen)

    def adnNodeOptFut = mNodeCache.maybeGetByIdCached(onNodeIdOpt)

    def reqPath: Option[String] = Some(request.uri)

    def saveStats: Future[_] = {
      val _gsiFut = gsiFut
      val _adnNodeOptFut = adnNodeOptFut
      val screenOpt = this.screenOpt
      val agentOs = this.agentOs

      for {
        gsiOpt      <- _gsiFut
        adnNodeOpt  <- _adnNodeOptFut
        res         <- {
          val isLocalClient = request.user.isSuper || gsiOpt.fold(false)(_.isLocalClient)
          val adStat = new MAdStat(
            clientAddr  = request.remoteAddress,
            action      = statAction.toString(),
            adIds       = madIds,
            adsRendered = adsCount,
            onNodeIdOpt = onNodeIdOpt,
            nodeName    = adnNodeOpt.map(_.meta.basic.name),
            ua          = uaOpt,
            personId    = personId,
            timestamp   = now,
            clIpGeo     = gsiOpt.flatMap(_.ipGeopoint),
            clTown      = gsiOpt.flatMap(_.cityName),
            clGeoLoc    = gsiOpt.flatMap(_.exactGeopoint),
            clCountry   = gsiOpt.flatMap(_.countryIso2),
            clLocAccur  = adSearchOpt
              .flatMap { a => a.geo.asGeoLocation.flatMap(_.accuracyMeters).map(_.toInt) },
            isLocalCl   = isLocalClient,
            clOSFamily  = agentOs
              .flatMap { os => Option(os.getFamilyName) }
              .filter(isStrUseful),
            clAgent     = agent
              .flatMap { _agent => Option(_agent.getName) }
              .filter(isStrUseful),
            clDevice    = agent
              .flatMap { _agent => Option(_agent.getDeviceCategory) }
              .flatMap { dc => Option(dc.getName) }
              .filter(isStrUseful),
            clickedAdIds = clickedAdIds,
            generation  = adSearchOpt.flatMap(_.randomSortSeed),
            clOsVsn     = agentOs
              .flatMap { os => Option(os.getVersionNumber) }
              .flatMap { vsn => Option(vsn.getMajor) }
              .filter(isStrUseful),
            clUid       = clUidOpt,
            scrOrient   = screenOpt
              .map(_.orientation.strId),
            scrResChoosen = screenOpt
              .flatMap(_.maybeBasicScreenSize)
              .map(_.toString()),
            pxRatioChoosen = screenOpt
              .map(_.pixelRatio.pixelRatio),
            viewportDecl = screenOpt
              .map(_.toString),
            reqUri = reqPath,
            ttl    = if(isLocalClient) Some(LOCAL_STAT_TTL) else None
          )
          // Отправляем на сохранение через соотв.подсистему.
          //trace(s"Saving stats: ${adStat.action} remote=${adStat.clientAddr} node=${adStat.onNodeIdOpt} ttl=${adStat.ttl}")
          scStatSaver.BACKEND.save(adStat)
        }
      } yield {
        None
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
  case class TilesStat(
    adSearch: AdSearch,
    madIds: Seq[String],
    gsiFut: Future[Option[GeoSearchInfo]]
  )(implicit val request: IReqHdr)
    extends StatT
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
  case class FocusedAdsStat(
    adSearch: AdSearch,
    madIds: Seq[String],
    override val withHeadAd: Boolean
  )(implicit val request: IReqHdr)
    extends StatT
  {
    override def gsiFut = adSearch.geo.geoSearchInfoOpt
    override val adSearchOpt = Some(adSearch)
    override def statAction = ScStatActions.Opened
  }


  /** Указываем, что тут рекламных карточек нет и не должно быть. */
  trait NoAdsStatT extends StatT {
    override def madIds: Seq[String] = Seq.empty
    override def adSearchOpt: Option[AdSearch] = None
  }


  /**
   * Записывалка статистики для обращения к findNode() экшену.
   * @param args Данные запроса поиска узлов.
   * @param gsiFut Асинхронные геоданные запроса.
   * @param request HTTP-Реквест.
   */
  case class NodeListingStat(
    args: MScNodeSearchArgs,
    gsiFut: Future[Option[GeoSearchInfo]]
  )(implicit val request: IReqHdr)
    extends StatT with NoAdsStatT
  {
    override def statAction = ScStatActions.Nodes
    override lazy val onNodeIdOpt: Option[String] = args.currAdnId
  }


  // ----- v2 статистика выдачи. ------

  def userSaOptFutFromRequest()(implicit req: IReqHdr): Future[Option[MAction]] = {
    userSaOptFut(req.user)
  }
  def userSaOptFut(user: ISioUser): Future[Option[MAction]] = {
    FutureUtil.optFut2futOpt( user.personIdOpt ) { personId =>
      for (personNodeOpt <- user.personNodeOptFut) yield {
        val maction = MAction(
          actions   = Seq( MActionTypes.CurrUser ),
          nodeId    = Seq( personId ),
          nodeName  = personNodeOpt.fold [Seq[String]] (Nil) (_.guessDisplayName.toSeq)
        )
        Some(maction)
      }
    }
  }


  /** Что-то типа builder'а для создания и сохранения одного элемента статистики второго поколения. */
  abstract class Stat2 {

    /** Контекст запроса. */
    def ctx: Context

    /** Сохраняемые stat actions. */
    def statActions: List[MAction]

    lazy val uaOpt = {
      ctx.request
        .headers
        .get(USER_AGENT)
        .map(_.trim)
        .filter(!_.isEmpty)
    }

    def browser = uaOpt.flatMap { ua =>
      // try-catch для самозащиты от возможных багов в православной либе uadetector.
      try {
        val uaParser = UADetectorServiceFactory.getResourceModuleParser
        Some(uaParser.parse(ua))
      } catch {
        case ex: Throwable =>
          warn("agent: Unable to use UADetector for parsing UA: " + ua, ex)
          None
      }
    }

    def clUidOpt: Option[String] = {
      statCookiesUtil
        .getFromRequest(ctx.request)
        .map { UuidUtil.uuidToBase64 }
    }


    /** stat action для описания текущего юзера. Можно получить его через userSaOptFut() или userSaOptFutFromRequest(). */
    def userSaOpt: Option[MAction]

    /** Оверрайдить при уже наличии нормального адреса. */
    def remoteAddr = geoIpUtil.fixRemoteAddr( ctx.request.remoteAddress )

    /** Является ли текущий клиент "локальным", т.е. не очень-то интересным для статистики. */
    def isLocalClient: Boolean = {
      remoteAddr.isLocal.contains(true) || ctx.request.user.isSuper
    }


    /** generation seed, если есть. */
    def gen: Option[Long] = None

    /** Скомпиленные под статистику данные юзер-агента. */
    def mua = {
      val _browser  = browser
      val browserOs = _browser.flatMap { _agent =>
        Option(_agent.getOperatingSystem)
      }
      MUa(
        ua      = uaOpt,
        browser = _browser
          .flatMap { _agent => Option(_agent.getName) }
          .filter(isStrUseful),
        device  = _browser
          .flatMap { _agent => Option(_agent.getDeviceCategory) }
          .flatMap { dc => Option(dc.getName) }
          .filter(isStrUseful),
        osFamily = browserOs
          .flatMap { os => Option(os.getFamilyName) }
          .filter(isStrUseful),
        osVsn = browserOs
          .flatMap { os => Option(os.getVersionNumber) }
          .flatMap { vsn => Option(vsn.getMajor) }
          .filter(isStrUseful)
      )
    }

    /** Перезаписать, если сейчас орудуем в каком-то другом домене, вне s.io. */
    def domain3p: Option[String] = None

    def mcommon: MCommon = {
      MCommon(
        component       = MComponents.Sc,
        ip              = Some( remoteAddr.remoteAddr ),
        clientUid       = clUidOpt,
        uri             = Option( ctx.request.uri ),
        domain3p        = domain3p,
        isLocalClient   = Some(isLocalClient),
        gen             = gen
      )
    }


    /** Данные по экрану. Оверрайдить при наличии уже распарсенного инстанса. */
    def devScreenOpt: Option[DevScreen] = ctx.deviceScreenOpt

    def vportQuanted: Option[MViewPort] = None

    def mscreen: MScreen = {
      val devScrOpt = devScreenOpt
      MScreen(
        orientation   = devScrOpt.map(_.orientation),
        vportReal     = devScrOpt.map(_.toStatViewPort),
        vportQuanted  = vportQuanted
      )
    }

    def locEnvOpt: Option[MLocEnv] = None
    def geoIpLoc: Option[IGeoFindIpResult] = None

    def mlocation: MLocation = {
      MLocation(
        geo = {
          val _geoOpt = locEnvOpt.flatMap(_.geoLocOpt)
          MGeoLocData(
            coords = _geoOpt
              .map(_.center),
            accuracy = _geoOpt
              .flatMap(_.accuracyOptM)
              .map(_.toInt)
          )
        },
        geoIp = {
          val _geoIpOpt = geoIpLoc
          MGeoLocData(
            coords    = _geoIpOpt
              .map(_.center),
            accuracy  = _geoIpOpt
              .flatMap(_.accuracyMetersOpt),
            town      = _geoIpOpt
              .flatMap(_.cityName),
            country   = _geoIpOpt
              .flatMap(_.countryIso2)
          )
        }
      )
    }

    /** Инстанс (пока несохраненный) статистики. */
    def mstat: MStat = {
      MStat(
        common    = mcommon,
        actions   = statActions ++ userSaOpt,
        timestamp = ctx.now,
        ua        = mua,
        screen    = mscreen,
        location  = mlocation
      )
    }

  }

  /** Отправить v2-статистику на сохранение в БД. */
  def saveStat(stat2: Stat2): Future[_] = {
    saveStat( stat2.mstat )
  }
  /** Отправить v2-статистику на сохранение в БД. */
  def saveStat(mstat: MStat): Future[_] = {
    playStatSaver.BACKEND
      .save(mstat)
  }

}

