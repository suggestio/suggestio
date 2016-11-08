package util.adv.geo

import com.google.inject.{Inject, Singleton}
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.order.MOrders
import io.suggest.model.geo.GeoPoint
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import models.mproj.ICommonDi
import models.req.IReqHdr
import util.PlayMacroLogsImpl
import util.geo.GeoIpUtil

import scala.concurrent.Future


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.09.16 15:19
  * Description: Утиль для форм георазмещения для нужд геолокации.
  */
@Singleton
class AdvGeoLocUtil @Inject() (
  geoIpUtil         : GeoIpUtil,
  mOrders           : MOrders,
  mItems            : MItems,
  mNodes            : MNodes,
  mCommonDi         : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._
  import slick.driver.api._


  /**
    * Для гео-размещений нужна начальная точка на карте, с которой начинается вся пляска.
    *
    * @param remoteAddress Адресок клиента.
    * @return Фьючерс с начальной гео-точкой.
    */
  def getGeoPointFromRemoteAddr(remoteAddress: String): Future[Option[GeoPoint]] = {
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
  def getGeoPointLastResort: GeoPoint = {
    // Штаб ВМФ СПб, который в центре СПб
    GeoPoint(59.93769, 30.30887)
  }

  private def _geoAdvsItemTypes = Seq(
    MItemTypes.GeoPlace.strId,
    MItemTypes.GeoTag.strId
  )

  /** Попытаться получить последние координаты текущей карточки из предыдущих размещений. */
  def getGeoPointFromAdsGeoAdvs(adIds: Traversable[String]): Future[Option[GeoPoint]] = {
    _getPointFromItemId(
      mItems.query
        .filter { q =>
          (q.adId inSet adIds) &&
            (q.iTypeStr inSet _geoAdvsItemTypes) &&
            q.geoShapeStrOpt.isDefined
        }
        .map(_.id)
        .max
    )
  }


  /**
    * Найти точку для указанного itemId
    * @param itemIdRep SQL-запрос, возвращающий item_id, содержащий искомую точку.
    *                  Чтобы избежать сортировки по id, ищем последний id ряда, содержащего геошейп.
    * @return
    */
  private def _getPointFromItemId(itemIdRep: Rep[Option[Gid_t]]): Future[Option[GeoPoint]] = {
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
      gsOpts.iterator
        .flatten
        .flatMap(_.centerPoint)
        .toStream
        .headOption
    }
  }


  /** Если вдруг не найдено размещений у текущей карточки, то поискать локации других карточек этого же продьюсера. */
  def getGeoPointFromProducer(producerIds: Seq[String], excludeAdIds: Seq[String]): Future[Option[GeoPoint]] = {
    // Найти id всех карточек этого продьюсера
    val prodAdsSearch = new MNodeSearchDfltImpl {
      override def nodeTypes = Seq( MNodeTypes.Ad )
      override def outEdges: Seq[ICriteria] = {
        val cr = Criteria(
          predicates  = Seq( MPredicates.OwnedBy ),
          // Заодно выставляем текущего юзера в id продьюсеров, вдруг чего...
          nodeIds     = producerIds
        )
        Seq(cr)
      }
      override def limit = 100
      // Отфильтровываем текущую карточку, т.к. по ней всё равно уже ничего не найдено.
      override def withoutIds = excludeAdIds
    }

    mNodes.dynSearchIds(prodAdsSearch)
      .flatMap( getGeoPointFromAdsGeoAdvs )
  }


  /** Попытаться найти геоточку исходя из предыдущих размещений юзера. */
  def getGeoPointFromUserGeoAdvs(contractId: Gid_t): Future[Option[GeoPoint]] = {
    // Найти контракт текущего юзера, по нему - id ордеров, по ордерам -- item'ы, и по ним уже точки.
    val orderIds = mOrders.query
      .filter { o =>
        o.contractId === contractId
      }
      .map(_.id)

    _getPointFromItemId(
      mItems.query
        .filter { i =>
          (i.orderId in orderIds) &&
            (i.iTypeStr inSet _geoAdvsItemTypes) &&
            i.geoShapeStrOpt.isDefined
        }
        .map(_.id)
        .max
    )
  }


  // Для возможности расширения кода вкупе с дедубликацией существующего кода, используется система детекторов локации.
  // Классы, реализующиие IGeoPointDetector, поочерёдно запускаются на исполнение в ожидании первого удачного результата.

  /** Интерфейс для одного детектора гео-точки в цепочке детекторов. */
  abstract class GeoPointDetector { that =>

    def get: Future[GeoPoint]

    def name: String = {
      // getSimpleName взрывоопасно!
      try {
        getClass.getSimpleName
      } catch { case ex: Throwable =>
        LOGGER.error("getSimpleName returned shit! Please check the code of inner classes", ex)
        "_$$$$FIX_ME_READ_LOGS_NOW$$$$_"
      }
    }


    def orElse(gpd: => GeoPointDetector): GeoPointDetector = {
      new GeoPointDetector {
        override def name = "orElse"
        override def get: Future[GeoPoint] = that.get.recoverWith { case ex: Throwable =>
          if (ex.isInstanceOf[NoSuchElementException]) {
            LOGGER.trace(s"Cannot detect using ${that.name}")
          } else {
            LOGGER.warn(s"Failed to detect using ${that.name}", ex)
          }
          gpd.get
        }
      }
    }
  }
  abstract class GeoPointDetectorOpt extends GeoPointDetector {
    def getOpt: Future[Option[GeoPoint]]
    override def get: Future[GeoPoint] = {
      getOpt.map( EmptyUtil.getF )
    }
  }


  object Detectors {

    /** Определение по предыдущим георазмещениям карточек. */
    case class FromAdsGeoAdvs(adIds: Traversable[String]) extends GeoPointDetectorOpt {
      override def getOpt: Future[Option[GeoPoint]] = {
        getGeoPointFromAdsGeoAdvs(adIds)
      }
    }

    /** Определение по георазмещениям продьюсера. */
    case class FromProducerGeoAdvs(producerIds: Seq[String], excludeAdIds: Seq[String] = Nil) extends GeoPointDetectorOpt {
      override def getOpt: Future[Option[GeoPoint]] = {
        getGeoPointFromProducer(producerIds, excludeAdIds)
      }
    }

    /**
      * Определить по другим размещенияем юзера.
      * @param contractIdOptFut см. [[models.req.IReq]].user, а именно [[models.req.ISioUser]].contractIdOptFut .
      */
    case class FromContractGeoAdvs(contractIdOptFut: Future[Option[Gid_t]]) extends GeoPointDetectorOpt {
      override def getOpt: Future[Option[GeoPoint]] = {
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
      override def getOpt: Future[Option[GeoPoint]] = {
        getGeoPointFromRemoteAddr(remoteAddr)
      }
    }

    /** Просто вернуть какую-то точку статическую. */
    case object FromDefaultGeoPoint extends GeoPointDetector {
      override def get: Future[GeoPoint] = {
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
            FromRemoteAddr( request.remoteAddress )
          }
          // Если ничего не удалось, то выставить совсем дефолтовую начальную точку.
          .orElse( FromDefaultGeoPoint )
      }
    }

  }

}
