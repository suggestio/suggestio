package util.stat

import javax.inject.Inject

import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.d2.MOrientations2d
import io.suggest.es.model.MEsUuId
import io.suggest.geo.{IGeoFindIpResult, MLocEnv}
import io.suggest.model.n2.node.MNode
import io.suggest.stat.m._
import io.suggest.stat.saver.PlayStatSaver
import io.suggest.util.UuidUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.{Context, ContextUtil}
import models.mproj.ICommonDi
import models.req.{IReqHdr, ISioUser}
import net.sf.uadetector.service.UADetectorServiceFactory
import play.api.http.HeaderNames.USER_AGENT
import play.mvc.Http.HeaderNames
import util.geo.GeoIpUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.14 14:37
  * Description: Утиль для упрощённой сборки статистики по HTTP-экшенам.
  *
  * Этот велосипед возник из-за необъятности модели MStat и необходимости заполнять её немного по-разному
  * в разных случаях ситуациях, при этом с минимальной дубликацией кода и легкой расширяемостью оного.
  */
class StatUtil @Inject()(
  statCookiesUtil         : StatCookiesUtil,
  playStatSaver           : PlayStatSaver,
  contextUtil             : ContextUtil,
  geoIpUtil               : GeoIpUtil,
  mCommonDi               : ICommonDi
)
  extends MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._


  /** Является ли указанная строка мусорной? */
  def isStrGarbage(str: String): Boolean = {
    (str == null) ||
      str.isEmpty ||
      str.equalsIgnoreCase("null") ||
      str.equalsIgnoreCase("undefined") ||
      str.equalsIgnoreCase("unknown")
  }

  def isStrUseful(str: String) = !isStrGarbage(str)


  // ----- v2 статистика выдачи. ------

  def userSaOptFutFromRequest()(implicit req: IReqHdr): Future[Option[MAction]] = {
    userSaOptFut(req.user)
  }
  def userSaOptFut(user: ISioUser): Future[Option[MAction]] = {
    FutureUtil.optFut2futOpt( user.personIdOpt ) { personId =>
      for (personNodeOpt <- user.personNodeOptFut) yield {
        val maction = personNodeStat(personId, personNodeOpt)
        Some(maction)
      }
    }
  }

  /** Собрать stat action для записи данных по текущему юзеру. */
  def personNodeStat(personId: String, personNodeOpt: Option[MNode]): MAction = {
    MAction(
      actions   = MActionTypes.Person :: Nil,
      nodeId    = personId :: Nil,
      nodeName  = personNodeOpt.fold [Seq[String]] (Nil) (_.guessDisplayName.toSeq)
    )
  }

  /** Завернуть данные по карточкам в stat-экшен. */
  def madsAction(mads: Seq[MNode], acType: MActionType): MAction = {
    MAction(
      actions   = Seq( acType ),
      nodeId    = mads.iterator
        .flatMap(_.id)
        .toSeq,
      nodeName  = mads.iterator
        .flatMap(_.guessDisplayName)
        .toSeq,
      count     = Seq( mads.size )
    )
  }

  def withNodeAction(acType: MActionType, nodeIdOpt: Option[MEsUuId], nodeOpt: Option[MNode])(acc0: List[MAction]): List[MAction] = {
    nodeIdOpt.fold(acc0) { nodeId =>
      MAction(
        actions   = Seq( acType ),
        nodeId    = Seq( nodeId ),
        nodeName  = nodeOpt.flatMap(_.guessDisplayName).toSeq,
        count     = Seq( nodeOpt.size )
      ) :: acc0
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
    def remoteAddr = geoIpUtil.fixRemoteAddr( ctx.request.remoteClientAddress )

    /** Является ли текущий клиент "локальным", т.е. не очень-то интересным для статистики. */
    def isLocalClient: Boolean = {
      remoteAddr.isLocal.contains(true) || ctx.request.user.isSuper
    }


    /** generation seed, если есть. */
    def gen: Option[Long] = None


    /** Определяем, тут у нас браузер ли или приложение или что-то ещё. */
    def uaTypes: List[MUaType] = {
      import MUaTypes._
      ctx.request.headers
        .get( HeaderNames.X_REQUESTED_WITH )
        .fold [List[MUaType]] (Browser :: Nil) { xRqWith =>
          val acc0 = App :: Nil
          if (xRqWith.endsWith(".Sio2m")) {
            CordovaApp :: acc0
          } else {
            debug(s"mua(): Header ${HeaderNames.X_REQUESTED_WITH} contains unknown value: $xRqWith ;;\n UA:$uaOpt\n from ${remoteAddr.remoteAddr}\n => ${ctx.request.uri}")
            acc0
          }
        }
    }


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
        osVsn    = browserOs
          .flatMap { os => Option(os.getVersionNumber) }
          .flatMap { vsn => Option(vsn.getMajor) }
          .filter(isStrUseful),
        uaType = uaTypes
      )
    }

    /** Перезаписать, если сейчас орудуем в каком-то другом домене, вне s.io. */
    def domain3p: Option[String] = _Domain.ifNotSio

    def components: List[MComponent] = Nil

    def uri = Option( ctx.request.uri )

    def mcommon: MCommon = {
      MCommon(
        components      = MComponents.Sc :: components,
        ip              = Some( remoteAddr.remoteAddr ),
        clientUid       = clUidOpt,
        uri             = uri,
        domain3p        = domain3p,
        isLocalClient   = Some(isLocalClient),
        gen             = gen
      )
    }


    /** Данные по экрану. Оверрайдить при наличии уже распарсенного инстанса. */
    def devScreenOpt = ctx.deviceScreenOpt

    def vportQuanted: Option[MViewPort] = None

    def mscreen: MScreen = {
      val devScrOpt = devScreenOpt
      MScreen(
        orientation   = for (devScr <- devScrOpt)
          yield MOrientations2d.forSize2d( devScr.wh ),
        vportPhys     = for (dso <- devScrOpt) yield {
          MViewPort(
            widthPx   = dso.wh.width,
            heightPx  = dso.wh.height,
            pxRatio   = Some( dso.pxRatio.pixelRatio )
          )
        },
        vportQuanted  = vportQuanted
      )
    }

    def locEnvOpt: Option[MLocEnv] = None
    def geoIpLoc: Option[IGeoFindIpResult] = None

    /** Подготовка stat-action'ов по маячкам. */
    def beaconsStats: Seq[MAction] = {
      val bcns = locEnvOpt
        .to( LazyList )
        .flatMap(_.bleBeacons)

      if (bcns.isEmpty) {
        Nil
      } else {
        val mact = MAction(
          actions = MActionTypes.BleBeaconNear :: Nil,
          nodeId  = bcns.map(_.uid),
          count   = bcns.map(_.distanceCm)
        )
        mact :: Nil
      }
    }

    /** Данные по геолокации отдельным полем. */
    def mlocation: MLocation = {
      MLocation(
        geo = {
          val _geoOpt = locEnvOpt.flatMap(_.geoLocOpt)
          MGeoLocData(
            coords = _geoOpt
              .map(_.point),
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

    def diag = MDiag.empty

    /** Инстанс (пока несохраненный) статистики. */
    def mstat: MStat = {
      MStat(
        common    = mcommon,
        actions   = Seq(
          statActions,
          userSaOpt.toSeq,
          beaconsStats
        ).flatten,
        timestamp = ctx.now,
        ua        = mua,
        screen    = mscreen,
        location  = mlocation,
        diag      = diag
      )
    }

    // toString() обычно не используется.
    // Если же будет часто вызываться, то лучше mstat сделать как lazy val вместо def.
    override def toString: String = {
      s"${classOf[Stat2].getSimpleName}(${mstat.toString})"
    }

    /** Утиль для быстрого задания значения domain3p. */
    object _Domain {
      def currentHost: Option[String] = {
        Option(ctx.request.host)
      }
      def ifNotSio: Option[String] = {
        currentHost
          .filterNot { contextUtil.SIO_HOSTS.contains }
      }
      def maybeCurrent(useCurrentHost: Boolean): Option[String] = {
        if (useCurrentHost)
          currentHost
        else
          None
      }
    }

  }


  // TODO X-Requested-With:io.suggest.Sio2m

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


/** Интерфейс DI-поля для доступа к [[StatUtil]]. */
trait IStatUtil {
  val statUtil: StatUtil
}
