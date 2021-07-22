package util.billing.cron

import java.time.OffsetDateTime
import io.suggest.es.model.EsModel
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.item.{IMItems, MItem}
import io.suggest.n2.node.{IMNodes, MNode}
import io.suggest.streams.StreamsUtil
import io.suggest.util.logs.MacroLogsImpl
import models.adv.build.{Acc, MCtxOuter, TryUpdateBuilder}
import models.mproj.IMCommonDi
import slick.sql.SqlAction
import util.adv.build.AdvBuilderFactoryDi

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.16 14:42
  * Description: AdvsUpdate -- система пакетного обновления размещений карточек.
  * Используется для активации и деактивации размещений, может использоваться и для других смежных задач.
  *
  * Абстракция класса позволяет реализовывать как инкрементальные апдейты, так и reinstall-обновления
  * со сборкой состояния размещений с чистого листа.
  *
  * Эта методика имеет серьезные плюсы для uninstall:
  * - в uninstall не требуется модифицировать карточку, только sql.
  * - меньше возможных ошибок и рассинхронизаций.
  * - Возможность замусоривать adv-эджи с чистой совестью,
  *   т.к. при любом adv uninstall всё равно всё будет правильно в итоге.
  * - Не требуется никаких itemId в эджах.
  *
  * Этапы работы:
  * 1. Поиск id карточек в MItems, для которых требуется обработка.
  *    Например, поиск карточек, которые пора снимать с публикации.
  * 2. Поиск item'ов для id карточки, которые требуется отработать.
  * 3. Обновление некоторых item'ов, например отключение.
  * 4. Редукция всех оставшихся item'ов на карточку.
  * 5. Сохранение итоговой карточки.
  */
abstract class AdvsUpdate
  extends MacroLogsImpl
  with IMCommonDi
  with AdvBuilderFactoryDi
  with IMItems
  with IMNodes
{

  val esModel: EsModel
  val streamsUtil: StreamsUtil

  import streamsUtil.Implicits._
  import mCommonDi._
  import slick.profile.api._
  import esModel.api._


  /** Частичные критерии выборки подходящих item'ов из таблицы. */
  def _itemsSql(i: mItems.MItemsTable): Rep[Option[Boolean]]

  val now = OffsetDateTime.now()


  /** Основная метод запуска всего модуля на исполнение.
    *
    * @return Фьючерс с кол-вом отработанных карточек.
    */
  def run(): Future[Int] = {
    lazy val logPrefix = s"run()#${now.toInstant.toEpochMilli}:"

    // запустить асинхронную подготовку общего контекста
    _builderCtxOuterFut

    slick.db
      // Поиск id карточек, которые нужно глянуть на следующей стадии.
      .stream {
        // Ищем только карточки, у которых есть offline ads с dateStart < now
        mItems.query
          .filter( _itemsSql )
          .map(_.nodeId)
          .distinct
          .result
          .forPgStreaming( 10 )
      }
      .toSource
      // Делаем всё последовательно, чтобы отладить наблюдающиеся проблемы с гео-тегами.
      .mapAsync(1) { nodeId =>
        runForNodeId( nodeId )
          .map(_ => true)
          .recover { case ex: Throwable =>
            LOGGER.error(s"$logPrefix Failed to process ad[$nodeId]", ex)
            false
          }
      }
      .runWith( streamsUtil.Sinks.count )
      .flatMap { countTotal =>
        if (countTotal > 0) {
          // Теги косячат при такой пакетной обработке. Надо паузу делать тут, рефреш индекса принудительный.
          // Иначе свежие теги НЕ находятся в индексе на последующих итерациях. TODO Актуально ли это ещё? Теги ребилдятся в отдельном потоке, вроде.
          LOGGER.info(s"$logPrefix Done, total processed: $countTotal")
          mNodes
            .refreshIndex()
            .recover { case ex: Throwable =>
              LOGGER.error(s"$logPrefix Can't refresh es-index", ex)
            }
            .map(_ => countTotal)
        } else {
          Future.successful( countTotal )
        }
      }
  }

  /** Фьючерс внешнего контекста для adv-билдера. */
  def builderCtxOuterFut: Future[MCtxOuter]
  final lazy val _builderCtxOuterFut = builderCtxOuterFut


  /** Есть ли item'ы для апдейта?
    * Просто для самоконтроля на случай race conditions.
    *
    * @param mitems итемы, среди которых происходит проверка.
    * @return true, если есть хотя бы один item, который требуют проработки, или когда просто неизвестно.
    *         false, если точно нет элементов, требующих обработки.
    */
  def hasItemsForProcessing(mitems: Iterable[MItem]): Boolean = {
    mitems.nonEmpty
  }

  /** 2. Поиск необходимых item'ов для указанного id карточки.
    *
    * @param adId id обрабатываемой карточки.
    * @return SqlAction.
    */
  def findItemsForAdId(adId: String, itypes: List[MItemType]): SqlAction[Iterable[MItem], NoStream, Effect.Read]

  /** 3. Билдинг размещения карточки.
    *
    * Пример реализации тела метода:
    * {{{
    *   val acc00Fut = Future.successful(tuData0.acc)
    *   val b00 = advBuilderFactory.builder(acc00Fut, now)
    *   for (acc2 <- mitems.foldLeft(b00)(...).accFut) yield {
    *     TryUpdateBuilder(acc2)
    *   }
    * }}}
    *
    * @param mitems Все item'ы, отправляемые на билд.
    * @param tuData0 Контекст апдейта. А adv-билдер надо собрать на его основе, если требуется.
    * @return Новый билдер.
    */
  def tryUpdateAd(tuData0: TryUpdateBuilder, mitems: Iterable[MItem]): Future[TryUpdateBuilder]

  /**
    * Запуск обработки размещений в рамках одной карточки.
    * Обработка картоки организована внутри транзакции.
    *
    * @param nodeId id обрабатываемого узла.
    */
  def runForNodeId(nodeId: String): Future[_] = {
    val madOptFut = mNodes.getByIdCache( nodeId )

    lazy val logPrefix = s"runForNodeId($nodeId)#${System.currentTimeMillis}:"
    LOGGER.trace(s"$logPrefix Starting...")

    // Нужны только item'ы, которые поддерживаются adv-билдерами
    val acc0Fut = for {
      madOpt <- madOptFut
    } yield {
      Acc(
        mnode       = madOpt.get,
        ctxOuterFut = _builderCtxOuterFut
      )
    }
    val b0 = advBuilderFactory.builder(acc0Fut, now)

    // Собрать экшен для изменений в БД биллинга.
    val updateAction = for {

      // Читаем все обрабатываемые item'ы, попутно блокируя для апдейта ниже по транзакции.
      mitems <- findItemsForAdId(nodeId, b0.supportedItemTypes).forUpdate
      if hasItemsForProcessing(mitems)

      // Блокировка транзакции в недо-экшене, готовящем db-экшены и обновляющий карточку.
      acc2 <- DBIO.from {
        for {
          acc0 <- acc0Fut
          // Это сложная операция: выстраивание набора изменений, наложение его на карточку, сохранение карточки, выстраивание экшенов SQL-транзакции:
          tuData1 <- esModel.tryUpdateM[MNode, TryUpdateBuilder]( mNodes, TryUpdateBuilder(acc0) ) { tuData =>
            tryUpdateAd(tuData, mitems)
          }
        } yield {
          tuData1.acc
        }
      }

      // Внести накопленные изменения в БД биллинга
      _ <- DBIO.seq( acc2.dbActions: _* )

    } yield {
      LOGGER.trace(s"$logPrefix Done")
      acc2.mnode
    }

    // Сохранить изменения во всех моделях, вернув итоговый фьючерс.
    val resFut = slick.db.run( updateAction.transactionally )

    // Залоггировать итоги работы:
    for {
      ex <- resFut.failed
      if ex.isInstanceOf[NoSuchElementException]
    } {
      // NSEE *обычно* значит, что hasItemsForProcessing() вернул false. Такое возможно, когда карточка была отработана одновременно с поиском.
      // ex НЕ логгируем, оно залогируется где-нибудь уровнем выше.
      for (madOpt <- madOptFut) {
        if (madOpt.nonEmpty) {
          LOGGER.warn(s"$logPrefix Possibly no items for ad[$nodeId], but they expected to be moments ago. Race conditions?")
        } else {
          // Какая-то инфа о размещении карточки (узла), которая уже удалена.
          LOGGER.warn(s"$logPrefix Node(ad) is missing, but zombie items is here. Purging zombies...")
          purgeItemsForAd(nodeId)
        }
      }
    }

    resFut
  }


  /** С каким item-статусом гасить зомби-итемы? */
  def purgeItemStatus: MItemStatus

  /**
    * Подавление активных item'ов для карточек, которые уже не существуют.
    * @param adId id рекламной карточки, требующей вмешательства.
    * @return
    */
  def purgeItemsForAd(adId: String): Future[Int] = {
    val fut = slick.db.run {
      mItems.query
        .filter { _.nodeId === adId }
        .filter(_itemsSql)
        .map { i =>
          (i.status, i.dateStatus, i.reasonOpt)
        }
        .update( (purgeItemStatus, OffsetDateTime.now(), Some("node gone away")) )
    }

    val logPrefix = s"purgeItemsForAd($adId):"

    fut.onComplete {
      case Success(count) =>
        LOGGER.info(s"$logPrefix Purged $count items for node")
      case Failure(ex) =>
        LOGGER.error(s"$logPrefix Failed to purge zombie items for current missing node", ex)
    }

    fut
  }

}
