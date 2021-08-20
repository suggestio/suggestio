package controllers.sc

import akka.stream.Materializer
import io.suggest.es.model.EsModel
import io.suggest.geo.{IGeoFindIpResult, MGeoLoc}
import io.suggest.n2.node.MNodes
import io.suggest.sc.sc3.{MSc3RespAction, MScQs}
import io.suggest.util.logs.MacroLogsImplLazy
import models.mctx.Context
import models.req.{IReq, IReqHdr}
import util.acl.{CanEditAd, IsNodeAdmin, MaybeAuth, SioControllerApi}
import util.ad.JdAdUtil
import util.adn.NodesUtil
import util.adv.geo.AdvGeoRcvrsUtil
import util.ble.BleUtil
import util.cdn.{CdnUtil, CorsUtil}
import util.geo.GeoIpUtil
import util.img.{DynImgUtil, LogoUtil, WelcomeUtil}
import util.n2u.N2NodesUtil
import util.showcase.{ScAdSearchUtil, ScSearchUtil, ShowcaseUtil}
import util.stat.StatUtil

import javax.inject.Inject
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:57
 * Description: Всякая базисная утиль для сборки Sc-контроллера.
 */
final class ScCtlUtil @Inject()(
                                val sioControllerApi: SioControllerApi,
                              ) {

  import sioControllerApi._


  // Imports for all UniApi-supported controllers.sc.Sc* classes:
  lazy val statUtil = injector.instanceOf[StatUtil]
  lazy val geoIpUtil = injector.instanceOf[GeoIpUtil]
  lazy val esModel = injector.instanceOf[EsModel]
  lazy val scUtil = injector.instanceOf[ShowcaseUtil]
  lazy val mNodes = injector.instanceOf[MNodes]
  lazy val scAdSearchUtil = injector.instanceOf[ScAdSearchUtil]
  lazy val canEditAd = injector.instanceOf[CanEditAd]
  lazy val jdAdUtil = injector.instanceOf[JdAdUtil]
  lazy val nodesUtil = injector.instanceOf[NodesUtil]
  lazy val n2NodesUtil = injector.instanceOf[N2NodesUtil]
  lazy val scSearchUtil = injector.instanceOf[ScSearchUtil]
  lazy val advGeoRcvrsUtil = injector.instanceOf[AdvGeoRcvrsUtil]
  lazy val cdnUtil = injector.instanceOf[CdnUtil]
  lazy val welcomeUtil = injector.instanceOf[WelcomeUtil]
  lazy val bleUtil = injector.instanceOf[BleUtil]
  lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  lazy val dynImgUtil = injector.instanceOf[DynImgUtil]
  lazy val logoUtil = injector.instanceOf[LogoUtil]
  lazy val maybeAuth = injector.instanceOf[MaybeAuth]
  lazy val corsUtil = injector.instanceOf[CorsUtil]

  implicit lazy val mat = injector.instanceOf[Materializer]


  /** Быстренькое добавление поля lazy val ctx в код sc-логики. */
  trait LazyContext {

    implicit def _request: IReq[_]

    implicit lazy val ctx: Context = getContext2

  }


  /** Всякая расшаренная утиль для сборки sc-логик. */
  trait LogicCommonT extends LazyContext { logic =>

    /** Частичная реализация Stat2 под нужды sc-логик. */
    abstract class Stat2 extends statUtil.Stat2 {
      override def ctx: Context = logic.ctx
    }

    /** Контекстно-зависимая сборка данных статистики. */
    def scStat: Future[Stat2]

    /** Сохранение подготовленной статистики обычно везде очень одинаковое. */
    def saveScStat(): Future[_] = {
      scStat
        .flatMap(statUtil.saveStat)
    }

  }


  case class GeoIpInfo(_qs: MScQs)(implicit request: IReqHdr) extends MacroLogsImplLazy {

    /** Подчищенные нормализованные данные о remote-адресе. */
    lazy val remoteIp = geoIpUtil.fixedRemoteAddrFromRequest

    /** Пошаренный результат ip-geo-локации. */
    lazy val geoIpResOptFut: Future[Option[IGeoFindIpResult]] = {
      val _remoteIp = remoteIp
      val findFut = geoIpUtil.findIpCached( _remoteIp.remoteAddr )
      if (LOGGER.underlying.isTraceEnabled()) {
        findFut.onComplete { res =>
          LOGGER.trace(s"$geoIpResOptFut geoIpResOptFut[${_remoteIp}]:: tried to geolocate by ip => $res")
        }
      }
      findFut
    }

    /** Результат ip-геолокации. приведённый к MGeoLoc. */
    lazy val geoIpLocOptFut = geoIpUtil.geoIpRes2geoLocOptFut( geoIpResOptFut )

    /** ip-геолокация, когда гео-координаты или иные полезные данные клиента отсутствуют. */
    lazy val reqGeoLocFut: Future[Option[MGeoLoc]] = {
      geoIpUtil.geoLocOrFromIp( _qs.common.locEnv.geoLocOpt )( geoIpLocOptFut )
    }

  }

}


/** Интерфейс для respAction-поля, которое часто присутствует в большинстве логик. */
trait IRespActionFut {

  def respActionFut: Future[MSc3RespAction]

}
