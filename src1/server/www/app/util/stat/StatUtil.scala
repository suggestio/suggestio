package util.stat

import javax.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.d2.MOrientations2d
import io.suggest.dev.{MOsFamilies, MOsFamily}
import io.suggest.es.model.MEsUuId
import io.suggest.geo.{IGeoFindIpResult, MLocEnv}
import io.suggest.n2.node.MNode
import io.suggest.proto.http.HttpConst
import io.suggest.stat.m._
import io.suggest.stat.saver.PlayStatSaver
import io.suggest.util.UuidUtil
import io.suggest.util.logs.MacroLogsImplLazy
import japgolly.univeq._
import models.mctx.{Context, ContextUtil}
import models.mproj.ICommonDi
import models.req.{IReqHdr, ISioUser}
import net.sf.uadetector.OperatingSystemFamily
import net.sf.uadetector.service.UADetectorServiceFactory
import play.api.http.HeaderNames.USER_AGENT
import play.mvc.Http.HeaderNames
import util.geo.GeoIpUtil

import scala.concurrent.Future
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.14 14:37
  * Description: Утиль для упрощённой сборки статистики по HTTP-экшенам.
  *
  * Этот велосипед возник из-за необъятности модели MStat и необходимости заполнять её немного по-разному
  * в разных случаях ситуациях, при этом с минимальной дубликацией кода и легкой расширяемостью оного.
  */
final class StatUtil @Inject()(
                                statCookiesUtil         : StatCookiesUtil,
                                playStatSaver           : PlayStatSaver,
                                contextUtil             : ContextUtil,
                                geoIpUtil               : GeoIpUtil,
                                mCommonDi               : ICommonDi
                              )
  extends MacroLogsImplLazy
{

  import mCommonDi._

  @inline implicit def osFamilyUe: UnivEq[OperatingSystemFamily] = UnivEq.force

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
      actions   = acType :: Nil,
      nodeId    = mads.iterator
        .flatMap(_.id)
        .toSeq,
      nodeName  = mads.iterator
        .flatMap(_.guessDisplayName)
        .toSeq,
      count     = mads.size :: Nil,
    )
  }

  def withNodeAction(acType: MActionType, nodeIdOpt: Option[MEsUuId], nodeOpt: Option[MNode])(acc0: List[MAction]): List[MAction] = {
    nodeIdOpt.fold(acc0) { nodeId =>
      MAction(
        actions   = acType :: Nil,
        nodeId    = nodeId :: Nil,
        nodeName  = nodeOpt
          .flatMap(_.guessDisplayName)
          .toSeq,
        count     = nodeOpt.size :: Nil,
      ) :: acc0
    }
  }


  /** Функкция-конвертер из net.sf.uadetector.OperationSystemFamily в MOsFamily. */
  def osFamilyConv: PartialFunction[OperatingSystemFamily, MOsFamily] = {
    case OperatingSystemFamily.ANDROID      => MOsFamilies.Android
    case OperatingSystemFamily.IOS          => MOsFamilies.Apple_iOS
  }


  /** Что-то типа builder'а для создания и сохранения одного элемента статистики второго поколения. */
  abstract class Stat2 {

    def logMsg: Option[String] = None

    /** Контекст запроса. */
    def ctx: Context

    /** Сохраняемые stat actions. */
    def statActions: List[MAction]

    lazy val uaOpt = {
      for {
        ua <- ctx.request
          .headers
          .get(USER_AGENT)
        uaTrimmed = ua.trim
        if uaTrimmed.nonEmpty
      } yield {
        uaTrimmed
      }
    }

    lazy val browser = uaOpt.flatMap { ua =>
      // try-catch для самозащиты от возможных багов в православной либе uadetector.
      Try {
        val uaParser = UADetectorServiceFactory.getResourceModuleParser
        Option( uaParser.parse(ua) )
      }
        .recover {
          case ex: Throwable =>
            LOGGER.warn(s"agent: Unable to use UADetector for parsing UA: $ua", ex)
            None
        }
        .toOption
        .flatten
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
      (remoteAddr.isLocal contains true) ||
      ctx.request.user.isSuper
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
          if (xRqWith.endsWith(".appsuggest") || xRqWith.endsWith(".Sio2m")) {
            CordovaApp :: acc0
          } else {
            if (xRqWith !=* HttpConst.Headers.X_REQUESTED_WITH_VALUE)
              LOGGER.debug(s"mua(): Header ${HeaderNames.X_REQUESTED_WITH} contains unknown value: $xRqWith ;;\n UA:$uaOpt\n from ${remoteAddr.remoteAddr}\n => ${ctx.request.uri}")
            acc0
          }
        }
    }

    /** Семейство ОСи на основе User-Agent в терминах модели s.io.
      *
      * Тут напрямую НЕ используется, а нужна в ScSite для формирования конфига выдачи
      * с предложением установить мобильное приложение.
      */
    def sioOsFamily: Option[MOsFamily] = {
      for {
        agent     <- browser
        os        <- Option( agent.getOperatingSystem )
        osFamily  <- Option( os.getFamily )
        mOsFamily <- osFamilyConv.lift( osFamily )
      } yield {
        mOsFamily
      }
    }

    /** Скомпиленные под статистику данные юзер-агента. */
    def mua = {
      val _browser  = browser
      val _browserOs = browser.flatMap { _agent =>
        Option( _agent.getOperatingSystem )
      }

      MUa(
        ua      = uaOpt,
        browser = for {
          agent <- _browser
          agentName <- Option(agent.getName)
          if isStrUseful(agentName)
        } yield agentName,
        device  = for {
          agent       <- _browser
          devCat      <- Option( agent.getDeviceCategory )
          devCatName  <- Option( devCat.getName )
          if isStrUseful( devCatName )
        } yield devCatName,
        osFamily = for {
          agentOs <- _browserOs
          osFamilyName <- Option( agentOs.getFamilyName )
          if isStrUseful( osFamilyName )
        } yield osFamilyName,
        osVsn    = for {
          agentOs <- _browserOs
          osVsn   <- Option( agentOs.getVersionNumber )
          osVsnMajor <- Option( osVsn.getMajor )
          if isStrUseful( osVsnMajor )
        } yield osVsnMajor,
        uaType = uaTypes
      )
    }

    /** Перезаписать, если сейчас орудуем в каком-то другом домене, вне s.io. */
    def domain3p: Option[String] = _Domain.ifNotSio

    def components: List[MComponent] = Nil

    def uri = Option( ctx.request.uri )

    def mcommon: MCommonStat = {
      MCommonStat(
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

    def mscreen: MStatScreen = {
      val devScrOpt = devScreenOpt
      MStatScreen(
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
    def mlocation: MStatLocation = {
      MStatLocation(
        geo = {
          val _geoOpt = locEnvOpt.flatMap(_.geoLocOpt)
          MGeoLocData(
            coords = for (geo <- _geoOpt) yield geo.point,
            accuracy = for (
              geo <- _geoOpt;
              accurM <- geo.accuracyOptM
            ) yield accurM.toInt,
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


  /** Сохранять ли всякий шум в mstat? */
  def SAVE_GARBAGE_TO_MSTAT = false
  lazy val mstats = current.injector.instanceOf[MStats]

  /** Всякий мусор (CSP-отчёты, Sc-отчёты об ошибках) можно сохранять или не сохранять.
    *
    * @param stat2 Данные статистики.
    * @return Фьючерс сохранения (или несохранения).
    */
  def maybeSaveGarbageStat(stat2: Stat2, logTail: => String = ""): Future[_] = {
    def msg = stat2.logMsg getOrElse "Remote info-report"

    if (SAVE_GARBAGE_TO_MSTAT) {
      // Сохраняем в базу отчёт об ошибке.
      for {
        merrId <- saveStat(stat2)
      } yield {
        LOGGER.trace(s"$msg saved to mstat[$merrId]." )
      }
    } else {
      LOGGER.info(s"$msg not saving, logging:\n${stat2.mstat.toString}${if (logTail.nonEmpty) "\n" else ""}$logTail")
      Future.successful(())
    }
  }


  /** Отправить v2-статистику на сохранение в БД. */
  def saveStat(stat2: Stat2): Future[_] = {
    playStatSaver.BACKEND
      .save( stat2.mstat )
  }

}


/** Интерфейс DI-поля для доступа к [[StatUtil]]. */
trait IStatUtil {
  val statUtil: StatUtil
}
