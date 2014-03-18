package util

import io.suggest.event._, SioNotifier.{Classifier, Subscriber}
import io.suggest.event.subscriber.SnClassSubscriber
import akka.actor.ActorContext
import models._, MMart.MartId_t
import SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.{Failure, Success}
import util.event.SiowebNotifier
import scala.concurrent.Future
import play.api.cache.Cache
import play.api.Play.current

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
      case YmMartAddedEvent(martId)   => handleMartAdd(martId)
      case YmMartDeletedEvent(martId) => handleMartDelete(martId)
    }
  }

  def snMap: Seq[(Classifier, Seq[Subscriber])] = {
    val subscribers = List(this)
    Seq(
      YmMartAddedEvent.getClassifier()    -> subscribers,
      YmMartDeletedEvent.getClassifier()  -> subscribers
    )
  }

  /** Асинхронные действия с индексами при создании ТЦ. */
  def handleMartAdd(martId: MartId_t): Future[MMartInx] = {
    // Создать индекс для указанного ТЦ
    val inxName = MART_INX_NAME_DFLT
    val inx = MMartInx(martId, inxName)
    val isFut = inx.save
    isFut onComplete {
      case Success(_)  => trace(s"inx2.MMartInx saved ok for mart=$martId at index $inxName")
      case Failure(ex) =>
        error(s"Failed to save inx2.MMartInx from mart=$martId at index $inxName", ex)
    }
    val smFut = inx.setMappings
    smFut onComplete {
      case Success(_)  => trace(s"Inx mapping set ok for mart=$martId")
      case Failure(ex) => error(s"Failed to set mapping for mart=$martId", ex)
      // TODO при ошибке надо сносить маппинг (или заливать с ignoreConflicts и снова сносить), а затем заливать заново по-нормальному.
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
        Future successful Some(mmartInx)

      case None =>
        val resultFut = MMartInx.getById(martId)
        resultFut onSuccess {
          case Some(mmartInx) => Cache.set(cacheKey, mmartInx, MART_INX_CACHE_SECONDS)
        }
        resultFut
    }
  }

}
