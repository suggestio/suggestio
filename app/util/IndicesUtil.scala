package util

import io.suggest.event._
import models._
import SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.{Failure, Success}
import util.event.SiowebNotifier
import scala.concurrent.Future
import play.api.cache.Cache
import play.api.Play.current
import io.suggest.util.SioEsUtil.laFuture2sFuture

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.14 17:22
 * Description: Разные фунции и прочая утиль для работы с индексами.
 */

object IndicesUtil extends PlayMacroLogsImpl {

  import LOGGER._

  implicit private def sn = SiowebNotifier

  /** Сколько времени надо кешировать в памяти частоиспользуемые метаданные по индексу ТЦ. */
  private val MART_INX_CACHE_SECONDS = current.configuration.getInt("inx2.mart.cache.seconds") getOrElse 60

  /** Сколько секунд кешировать в памяти магазин. */
  private val SHOP_CACHE_SECONDS = current.configuration.getInt("mshop.cache.seconds") getOrElse 60

  /** Имя дефолтового индекса для индексации MMart. Используется пока нет менеджера индексов,
    * и одного индекса на всех достаточно. */
  val MART_INX_NAME_DFLT = "--1siomart"


  /** Реакция на включение/отключение магазина. */
  private def handleShopOnOff(soe: MShopOnOffEvent) {
    val mshopFut = getShopByIdCached(soe.shopId).map(_.get)
    val mmartInxFut = mshopFut flatMap { mshop =>
      getInxFormMartCached(mshop.martId.get)
    }
    // В зависимости от нового состояния магазина, залить или выпилить его выдачу.
    val fut = if (soe.isEnabled) {
      mshopFut flatMap { mshop =>
        handleShopEnable(mshop, mmartInxFut)
      }
    } else {
      handleShopDisable(soe.shopId, mmartInxFut)
    }
    lazy val logPrefix = s"handleShopOnOff($soe): "
    fut onComplete {
      case Success(adsProcessed) => trace(logPrefix + s"shop on/off: OK. $adsProcessed ads processed.")
      case Failure(ex) => error(logPrefix + "Shop on/off failed.", ex)
    }
  }

  /** Реакция на сохранение магазина. */
  private def handleShopSaved(mshop: MShop) {
    Cache.set(cacheKeyForShop(mshop.id.get), mshop, SHOP_CACHE_SECONDS)
    lazy val logPrefix = s"handleShopSaved(${mshop.idOrNull}): "
    // При изменении сеттингов магазина надо пересохранять все рекламы магазина.
    // TODO Имеет смысл как-то определять ситуации, когда сеттинги не изменены. Это поможет избежать лишних переиндексаций.
    val mmartInxFut = getInxFormMartCached(mshop.martId.get)
    val itemsReadyFut: Future[Int] = if (mshop.settings.supIsEnabled) {
      handleShopEnable(mshop, mmartInxFut)
    } else {
      // Магазин отключен. Надо удалить все рекламные карточки этого магазина из выдачи.
      handleShopDisable(mshop.id.get, mmartInxFut)
    }
    itemsReadyFut onComplete {
      case Success(itemsProcessed) =>
        if (itemsProcessed > 0)
          trace(logPrefix + s"Shop ads processing finished. $itemsProcessed items processed.")
      case Failure(ex) =>
        error(logPrefix + "Failed to process shop ads", ex)
    }
  }

  /** Реакция на выключение магазина. */
  private def handleShopDisable(shopId: ShopId_t, mmartInxFut: Future[Option[MMartInx]]): Future[Int] = {
    mmartInxFut flatMap {
      case Some(mmartInx) =>
        MMartAdIndexed.deleteByShop(shopId, mmartInx)
      case None =>
        Future successful 0
    }
  }

  /** Реакция на включение магазина. Надо залить карточки магазина в выдачу. */
  private def handleShopEnable(mshop: MShop, mmartInxFut: Future[Option[MMartInx]]): Future[Int] = {
    val logPrefix = s"handleShopEnable(${mshop.id.get}): "
    val showShowLevels = mshop.getAllShowLevels
    // Нужно залить в хранилище карточки магазина
    MMartAd.findForShop(mshop.id.get) flatMap { ads =>
      mmartInxFut flatMap { mmartInxOpt =>
        Future.traverse(ads) { mad =>
          // Нужно сделать примерно тоже, что и в handleAdSaved
          maybeCollectUserCatStr(mad.userCatId) map { userCatsStrs =>
            val adShowLevels = mad.showLevels intersect showShowLevels
            val mmadI = MMartAdIndexed(mad, userCatsStrs, adShowLevels, mmartInxOpt.get)
            mmadI.indexRequestBuilder
          }
        } flatMap { adsIrbs =>
          // Набор index-реквестов обернуть в bulk и отправить.
          if (adsIrbs.isEmpty) {
            trace(logPrefix + "Shop has no ads. Nothing to reindex.")
            Future successful 0
          } else {
            val adsTotal = adsIrbs.size
            trace(logPrefix + s"Bulk indexing of $adsTotal ads...")
            val bulk = client.prepareBulk()
            adsIrbs foreach { bulk.add }
            bulk.execute().map { brr =>
              if (brr.hasFailures)
                error(logPrefix + s"Bulk request ($adsTotal items) finished with errors:\n  " + brr.buildFailureMessage)
              else
                trace(logPrefix + s"Bulk request finished ok. $adsTotal ads indexed ok. Took ${brr.getTookInMillis} ms.")
              adsTotal
            }
          }
        }
      }
    }
  }


  /** Сформулировать строку для индексируемого поля userCatStr. */
  // TODO Перенести в MMartAd, когда MMartCat будет вынесена в sioutil.
  private def collectUserCatStr(baseCatId: String): Future[List[String]] = {
    MMartCategory.foldUpChain [List[String]] (baseCatId, Nil) { (acc, mmc) =>
      if (mmc.includeInAll)
        mmc.name :: acc
      else
        acc
    } map { _.reverse }
  }

  private def maybeCollectUserCatStr: PartialFunction[Option[String], Future[List[String]]] = {
    case Some(catId) => collectUserCatStr(catId)
    case None        => Future successful Nil
  }


  /** Генератор cache-ключа для сохранения считанного MMartInx. */
  private def cacheKeyForMartInx(martId: MartId_t) = martId + ".martInx"

  /**
   * Асинхронно узнать метаданные индекса ТЦ, кешируя результат.
   * @param martId id ТЦ.
   * @return Фьючерс, соответствующий результату MMartInx.getById().
   */
  private def getInxFormMartCached(martId: MartId_t): Future[Option[MMartInx]] = {
    val cacheKey = cacheKeyForMartInx(martId)
    Cache.getAs[MMartInx](cacheKey) match {
      case Some(mmartInx) =>
        Future successful Option(mmartInx)

      case None =>
        val resultFut = MMartInx.getById(martId)
        resultFut onSuccess {
          case Some(mmartInx) => Cache.set(cacheKey, mmartInx, MART_INX_CACHE_SECONDS)
        }
        resultFut
    }
  }


  @deprecated("Use MAdnNodeCache instead", "2014.apr.08")
  private def cacheKeyForShop(shopId: ShopId_t) = shopId + ".shop"

  /**
   * Асинхронно прочитать MShop из кеша или из хранилища.
   * @param shopId id магазина.
   * @return Тоже самое, что и [[io.suggest.ym.model.MShop.getById()]].
   */
  @deprecated("Use MAdnNodeCache instead", "2014.apr.08")
  private def getShopByIdCached(shopId: ShopId_t): Future[Option[MShop]] = {
    val cacheKey = cacheKeyForShop(shopId)
    Cache.getAs[MShop](cacheKey) match {
      case Some(mshop) =>
        Future successful Option(mshop)

      case None =>
        val resultFut = MShop.getById(shopId)
        resultFut onSuccess {
          case Some(mshop) => Cache.set(cacheKey, mshop, SHOP_CACHE_SECONDS)
        }
        resultFut
    }
  }

}
