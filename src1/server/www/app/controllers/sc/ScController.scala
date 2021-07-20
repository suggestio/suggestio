package controllers.sc

import io.suggest.es.model.EsModel
import io.suggest.geo.{IGeoFindIpResult, MGeoLoc}
import io.suggest.sc.sc3.{MSc3RespAction, MScQs}
import io.suggest.util.logs.MacroLogsImplLazy

import javax.inject.{Inject, Singleton}
import models.mctx.Context
import models.mproj.IMCommonDi
import models.req.{IReq, IReqHdr}
import util.acl.SioControllerApi
import util.cdn.ICdnUtilDi
import util.geo.IGeoIpUtilDi
import util.stat.{IStatUtil, StatUtil}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:57
 * Description: Всякая базисная утиль для сборки Sc-контроллера.
 */
@Singleton
final class ScCtlApi @Inject()(
                                sioControllerApi: SioControllerApi,
                              ) {

  import sioControllerApi._
  import sioControllerApi.mCommonDi.current.injector

  protected lazy val statUtil = injector.instanceOf[StatUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

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


  /** Интерфейс для respAction-поля, которое часто присутствует в большинстве логик. */
  trait IRespActionFut {

    def respActionFut: Future[MSc3RespAction]

  }

}


// deprecated
trait ScController
  extends IMCommonDi
  with ICdnUtilDi
  with IStatUtil
  with IGeoIpUtilDi
{

  val sioControllerApi: SioControllerApi
  val esModel: EsModel

  import mCommonDi.ec
  import sioControllerApi.request2Messages


  /** Быстренькое добавление поля lazy val ctx в код sc-логики. */
  protected trait LazyContext {

    implicit def _request: IReq[_]

    implicit lazy val ctx: Context = sioControllerApi.getContext2

  }


  class GeoIpInfo(_qs: MScQs)(implicit request: IReqHdr) extends MacroLogsImplLazy {

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


  /** Всякая расшаренная утиль для сборки sc-логик. */
  protected trait LogicCommonT extends LazyContext { logic =>

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


  /** Интерфейс для respAction-поля, которое часто присутствует в большинстве логик. */
  protected trait IRespActionFut {

    def respActionFut: Future[MSc3RespAction]

  }

}