package util

import io.suggest.event._, SioNotifier.{Classifier, Subscriber}
import io.suggest.event.subscriber.SnClassSubscriber
import akka.actor.ActorContext
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

object IndicesUtil extends PlayMacroLogsImpl with SNStaticSubscriber with SnClassSubscriber {

  import LOGGER._

  implicit private def sn = SiowebNotifier

  /** Сколько времени надо кешировать в памяти частоиспользуемые метаданные по индексу ТЦ. */
  val MART_INX_CACHE_SECONDS = current.configuration.getInt("inx2.mart.cache.seconds") getOrElse 60

  /** Сколько секунд кешировать в памяти магазин. */
  val SHOP_CACHE_SECONDS = current.configuration.getInt("mshop.cache.seconds") getOrElse 60

  /** Имя дефолтового индекса для индексации MMart. Используется пока нет менеджера индексов,
    * и одного индекса на всех достаточно. */
  val MART_INX_NAME_DFLT = "--1siomart"


  /**
   * Передать событие подписчику.
   * @param event событие.
   * @param ctx контекст sio-notifier.
   */
  def publish(event: SioNotifier.Event)(implicit ctx: ActorContext) {
    event match {
      case mae: YmMartAddedEvent      => handleMartAdd(mae.martId)
      case mde: YmMartDeletedEvent    => handleMartDelete(mde.martId)
      case ase: AdSavedEvent          => handleAdSaved(ase.mmartAd)
      case ade: AdDeletedEvent        => handleAdDeleted(ade.mmartAd)
      case msse: MShopSavedEvent      => handleShopSaved(msse.mshop)
      case soe: MShopOnOffEvent       => handleShopOnOff(soe)
    }
  }

  def snMap: Seq[(Classifier, Seq[Subscriber])] = {
    val subscribers = List(this)
    Seq(
      YmMartAddedEvent.getClassifier()    -> subscribers,
      YmMartDeletedEvent.getClassifier()  -> subscribers,
      AdSavedEvent.getClassifier()        -> subscribers,
      MShopSavedEvent.getClassifier()     -> subscribers,
      MShopOnOffEvent.getClassifier()     -> subscribers
    )
  }

  /** Асинхронные действия с индексами при создании ТЦ. */
  def handleMartAdd(martId: MartId_t): Future[MMartInx] = {
    val logPrefix = s"handleMartAdd($martId): "
    // Создать индекс для указанного ТЦ
    val inxName = MART_INX_NAME_DFLT
    val inx = MMartInx(martId, inxName)
    val isFut = inx.save
    isFut onComplete {
      case Success(_)  => trace(logPrefix + "inx2.MMartInx saved ok for index " + inxName)
      case Failure(ex) => error(logPrefix + "Failed to save inx2.MMartInx from index " + inxName, ex)
    }
    val smFut = inx.setMappings
    smFut onComplete {
      case Success(_)  => trace(logPrefix + "Inx mapping set ok for mart")
      case Failure(ex) => error(logPrefix + "Failed to set mapping for mart", ex)
      // TODO при ошибке надо сносить маппинг (или заливать с ignoreConflicts и снова сносить), а затем заливать заново по-нормальному.
    }
    // Возможно, в ТЦ уже есть данные для индексации.
    smFut flatMap { _ =>
      // Пробежаться по карточкам ТЦ и сымитировать их добавление.
      MMartAd.findForMartRt(martId, shopMustMiss = true) flatMap { martAds =>
        Future.traverse(martAds) { handleAdSaved }
      } onComplete {
        case Success(l)  => trace(logPrefix + s"martAds: ${l.size} mart ads re-processed for index " + inxName)
        case Failure(ex) => error(logPrefix + s"martAds: Failed to process ads", ex)
      }
      // Нужно пробежаться по включенным магазинам и сымитировать shopEnabled
      MShop.findByMartId(martId) flatMap { mshops =>
        if (!mshops.isEmpty) {
          trace(logPrefix + "added mart already has shops. Loading enabled shops into index " + inxName)
          val inxOptFut = Future successful Some(inx)
          Future.traverse(mshops) { mshop =>
            if (mshop.settings.supIsEnabled) {
              handleShopEnable(mshop, inxOptFut)
            } else {
              Future successful 0
            }
          }
        } else {
          Future successful Nil
        }
      }
    } onComplete {
      case Success(results) if !results.isEmpty =>
        debug(s"${logPrefix}Successfully processed ${results.size} mart's shops, loaded ${results.count(_ != 0)} shops with ${results.sum} ads total.")
      case Failure(ex) => error(logPrefix + "Failed to load pre-existing mart's shops into index.", ex)
      case _ => // Do nothing
    }
    for {
      _ <- isFut
      _ <- smFut
    } yield inx
  }

  /** Асинхронные действия с индексами при удалении ТЦ. */
  def handleMartDelete(martId: MartId_t): Future[_] = {
    lazy val logPrefix = s"handleMartDelete($martId): "
    // Удалить индекс, созданный для указанного ТЦ
    MMartInx.getById(martId) flatMap {
      case Some(mmartInx) =>
        trace(s"${logPrefix}inx = $mmartInx :: Erasing index mappings...")
        val dmFut = mmartInx.deleteMappings
        dmFut onComplete {
          case Success(_) =>
            trace(logPrefix + "Erasing index mappings fininshed. Erasing inx2 metadata...")
            mmartInx.delete onComplete {
              case Success(_)   => trace(logPrefix + "inx2 metadata erased ok.")
              case Failure(ex2) => error(logPrefix + "Failed to delete inx2 metadata", ex2)
            }

          case Failure(ex1) => error(logPrefix + "Failed to erase mappings", ex1)
        }
        dmFut

      case None =>
        warn(s"${logPrefix}No inx2 found for mart=$martId - Nothing to erase.")
        Future successful ()
    }
  }

  /**
   * Оригинал экземпляра MMartAd сохранен в хранилище.
   * Нужно посмотреть в настройки публикации картинки и магазина, и добавить/удалить из выдачи эту рекламу.
   * @param mmartAd Экземпляр рекламной карточки.
   */
  private def handleAdSaved(mmartAd: MMartAd): Future[_] = {
    lazy val logPrefix = s"handleAdSave(${mmartAd.id.get}): "
    val martInx2Fut = getInxFormMartCached(mmartAd.receiverIds).map(_.get)
    val userCatStrFut = maybeCollectUserCatStr(mmartAd.userCatId)
    // Реклама бывает на уровне ТЦ и на уровне магазина. Если на уровне магазина, то надо определить допустимые уровни отображения.
    // Нужно пропатчить showLevels согласно допустимым уровням магазина.
    val allowedDisplayLevelsFut: Future[Set[AdShowLevel]] = mmartAd.producerId match {
      // Это реклама от магазина.
      case Some(_shopId) =>
        getShopByIdCached(_shopId) map {
          case Some(mshop)  => mshop.getAllShowLevels
          case None         => Set.empty
        }

      // Это реклама от ТЦ. Пока её можно отображать всегда.
      case None => Future successful MMartAd.MART_ALWAYS_SHOW_LEVELS
    }
    for {
      allowedDisplayLevels <- allowedDisplayLevelsFut
      mmartInx2 <- martInx2Fut
    } yield {
      val shownLevels = mmartAd.showLevels intersect allowedDisplayLevels
      if (shownLevels.isEmpty) {
        MMartAdIndexed.deleteById(mmartAd.id.get, mmartInx2) onComplete {
          case Success(result)  => trace(logPrefix + "Ad deleted/hidden from indexing -> " + result)
          case Failure(ex)      => error(logPrefix + "Failed to erase/hide AD from indexing", ex)
        }
      } else {
        userCatStrFut map { userCatStrs =>
          val mmai = MMartAdIndexed(mmartAd, userCatStrs, shownLevels, mmartInx2)
          //trace(logPrefix + "Saving to index: " + mmai)
          mmai.save onComplete {
            case Success(savedAdId) => trace(logPrefix + "Ad inserted/updated into indexing: " + mmartInx2)
            case Failure(ex)        => error(logPrefix + "Faild to save AD into index", ex)
          }
        }
      }
    }
  }

  /** Реакция на удаление рекламной карточки: нужно её удалить из индекса карточек. */
  private def handleAdDeleted(mmartAd: MMartAd) {
    getInxFormMartCached(mmartAd.receiverIds) onSuccess {
      case Some(mmartInx) => MMartAdIndexed.deleteById(mmartAd.id.get, mmartInx)
    }
  }

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
  def getInxFormMartCached(martId: MartId_t): Future[Option[MMartInx]] = {
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
  def getShopByIdCached(shopId: ShopId_t): Future[Option[MShop]] = {
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
