package util.adv.geo

import javax.inject.Inject
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.geo.MGeoPoint
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.order.MOrders
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNodeTypes, MNodes}
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.IReqHdr
import util.geo.GeoIpUtil

import scala.concurrent.Future


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.09.16 15:19
  * Description: Утиль для форм георазмещения для нужд геолокации.
  */
final class AdvGeoLocUtil @Inject() (
                                      mCommonDi         : ICommonDi
                                    )
  extends MacroLogsImpl
{

  import mCommonDi.current.injector

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val geoIpUtil = injector.instanceOf[GeoIpUtil]
  private lazy val mOrders = injector.instanceOf[MOrders]
  private lazy val mItems = injector.instanceOf[MItems]
  private lazy val mNodes = injector.instanceOf[MNodes]

  import mCommonDi._
  import slick.profile.api._


  /**
    * Для гео-размещений нужна начальная точка на карте, с которой начинается вся пляска.
    *
    * @param remoteAddress Адресок клиента.
    * @return Фьючерс с начальной гео-точкой.
    */
  def getGeoPointFromRemoteAddr(remoteAddress: String): Future[Option[MGeoPoint]] = {
    val raInfo = geoIpUtil.fixRemoteAddr( remoteAddress )
    for {
      findIpResOpt <- geoIpUtil.findIpCached( raInfo.remoteAddr )
    } yield {
      for (r <- findIpResOpt) yield {
        r.center
      }
    }
  }

  /** Когда нет точки для отображения, взять её с потолка. */
  def getGeoPointLastResort: MGeoPoint = {
    MGeoPoint.Examples.RU_SPB_CENTER
  }

  private def _geoAdvsItemTypes = Seq(
    MItemTypes.GeoPlace.value,
    MItemTypes.GeoTag.value
  )

  private def _traceAsyncRes[R](resFut: Future[R], msg: => String): Unit = {
    if (LOGGER.underlying.isTraceEnabled()) {
      resFut.onComplete { res =>
        LOGGER.trace(s"$msg: => $res")
      }
    }
  }

  /** Попытаться получить последние координаты текущей карточки из предыдущих размещений. */
  def getGeoPointFromAdsGeoAdvs(adIds: Iterable[String]): Future[Option[MGeoPoint]] = {
    val resFut = _getPointFromItemId(
      mItems.query
        .filter { q =>
          (q.nodeId inSet adIds) &&
            (q.iTypeStr inSet _geoAdvsItemTypes) &&
            q.geoShapeStrOpt.isDefined
        }
        .map(_.id)
        .max
    )

    _traceAsyncRes(resFut, s"getGeoPointFromAdsGeoAdvs($adIds)")
    resFut
  }


  /**
    * Найти точку для указанного itemId
    * @param itemIdRep SQL-запрос, возвращающий item_id, содержащий искомую точку.
    *                  Чтобы избежать сортировки по id, ищем последний id ряда, содержащего геошейп.
    * @return
    */
  private def _getPointFromItemId(itemIdRep: Rep[Option[Gid_t]]): Future[Option[MGeoPoint]] = {
    for {
      gsOpts <- slick.db.run {
        // Извлекаем столбец последнего ряда по его id...
        mItems.query
          .filter { q =>
            q.id === itemIdRep
          }
          .map(_.geoShapeOpt)
          .result
      }
    } yield {
      gsOpts
        .iterator
        .flatten
        .flatMap(_.centerPoint)
        .nextOption()
    }
  }


  /** Если вдруг не найдено размещений у текущей карточки, то поискать локации других карточек этого же продьюсера. */
  def getGeoPointFromProducer(producerIds: Seq[String], excludeAdIds: Seq[String]): Future[Option[MGeoPoint]] = {
    import esModel.api._

    // Найти id всех карточек этого продьюсера
    val prodAdsSearch = new MNodeSearch {
      override val nodeTypes = MNodeTypes.Ad :: Nil
      override val outEdges: MEsNestedSearch[Criteria] = {
        val cr = Criteria(
          predicates  = MPredicates.OwnedBy :: Nil,
          // Заодно выставляем текущего юзера в id продьюсеров, вдруг чего...
          nodeIds     = producerIds
        )
        MEsNestedSearch.plain( cr )
      }
      override def limit = 100
      // Отфильтровываем текущую карточку, т.к. по ней всё равно уже ничего не найдено.
      override def withoutIds = excludeAdIds
    }

    val resFut = mNodes
      .dynSearchIds(prodAdsSearch)
      .flatMap( getGeoPointFromAdsGeoAdvs )

    _traceAsyncRes(resFut, s"getGeoPointFromProducer($producerIds, excl=$excludeAdIds)")
    resFut
  }


  /** Попытаться найти геоточку исходя из предыдущих размещений юзера. */
  def getGeoPointFromUserGeoAdvs(contractId: Gid_t): Future[Option[MGeoPoint]] = {
    // Найти контракт текущего юзера, по нему - id ордеров, по ордерам -- item'ы, и по ним уже точки.
    val orderIds = mOrders.query
      .filter { o =>
        o.contractId === contractId
      }
      .map(_.id)

    val resFut = _getPointFromItemId(
      mItems.query
        .filter { i =>
          (i.orderId in orderIds) &&
            (i.iTypeStr inSet _geoAdvsItemTypes) &&
            i.geoShapeStrOpt.isDefined
        }
        .map(_.id)
        .max
    )

    _traceAsyncRes(resFut, s"getGeoPointFromUserGeoAdvs($contractId)")
    resFut
  }


  // Для возможности расширения кода вкупе с дедубликацией существующего кода, используется система детекторов локации.
  // Классы, реализующиие IGeoPointDetector, поочерёдно запускаются на исполнение в ожидании первого удачного результата.

  /** Интерфейс для одного детектора гео-точки в цепочке детекторов. */
  abstract class GeoPointDetector { that =>

    def get: Future[MGeoPoint]

    def orElse(gpd: => GeoPointDetector): GeoPointDetector = {
      new GeoPointDetector {
        override def toString = s"$that.orElse($gpd)"
        override def get: Future[MGeoPoint] = {
          val fut0 = that.get
          // Залоггировать результат работы, если требуется
          _traceAsyncRes(fut0, that.toString)

          // Повесить recovery callback
          fut0.recoverWith { case ex: Throwable =>
            if (ex.isInstanceOf[NoSuchElementException]) {
              LOGGER.trace(s"No data from $that")
            } else {
              LOGGER.warn(s"Failed to detect using $that", ex)
            }
            gpd.get
          }
        }
      }
    }
  }
  abstract class GeoPointDetectorOpt extends GeoPointDetector {
    def getOpt: Future[Option[MGeoPoint]]
    override def get: Future[MGeoPoint] = {
      getOpt.map( EmptyUtil.getF )
    }
  }


  object Detectors {

    /** Определение по предыдущим георазмещениям карточек. */
    case class FromAdsGeoAdvs(adIds: Iterable[String]) extends GeoPointDetectorOpt {
      override def getOpt: Future[Option[MGeoPoint]] = {
        getGeoPointFromAdsGeoAdvs(adIds)
      }
    }

    /** Определение по георазмещениям продьюсера. */
    case class FromProducerGeoAdvs(producerIds: Seq[String], excludeAdIds: Seq[String] = Nil) extends GeoPointDetectorOpt {
      override def getOpt: Future[Option[MGeoPoint]] = {
        getGeoPointFromProducer(producerIds, excludeAdIds)
      }
    }

    /**
      * Определить по другим размещенияем юзера.
      * @param contractIdOptFut см. [[models.req.IReq]].user, а именно [[models.req.ISioUser]].contractIdOptFut .
      */
    case class FromContractGeoAdvs(contractIdOptFut: Future[Option[Gid_t]]) extends GeoPointDetectorOpt {
      override def getOpt: Future[Option[MGeoPoint]] = {
        contractIdOptFut
          .flatMap { contractIdOpt =>
            FutureUtil.optFut2futOpt(contractIdOpt) { contractId =>
              getGeoPointFromUserGeoAdvs(contractId)
            }
          }
      }
    }

    /** Определить по geoip */
    case class FromRemoteAddr(remoteAddr: String) extends GeoPointDetectorOpt {
      override def getOpt: Future[Option[MGeoPoint]] = {
        getGeoPointFromRemoteAddr(remoteAddr)
      }
    }

    /** Просто вернуть какую-то точку статическую. */
    case object FromDefaultGeoPoint extends GeoPointDetector {
      override def get: Future[MGeoPoint] = {
        Future.successful( getGeoPointLastResort )
      }
    }

    /** common-case: добавить определение по контракту, geoip и fallback-значение. */
    implicit class OrFromReqOrDefault(that: GeoPointDetector)(implicit request: IReqHdr) {
      def orFromReqOrDflt: GeoPointDetector = {
        that
          // Ищем начальную точку среди других размещений текущего юзера
          .orElse {
            FromContractGeoAdvs( request.user.contractIdOptFut )
          }
          // Ищем начальную точку карты из geoip
          .orElse {
            FromRemoteAddr( request.remoteClientAddress )
          }
          // Если ничего не удалось, то выставить совсем дефолтовую начальную точку.
          .orElse( FromDefaultGeoPoint )
      }
    }

  }

}
