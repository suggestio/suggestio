package util.billing.cron

import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.mbill2.m.item.{IMItems, MItem}
import io.suggest.model.es.EsModelUtil
import models.MNode
import models.adv.build.{MCtxOuter, Acc, TryUpdateBuilder}
import models.mproj.IMCommonDi
import org.joda.time.DateTime
import slick.profile.SqlAction
import util.PlayMacroLogsImpl
import util.adv.build.AdvBuilderFactoryDi

import scala.concurrent.Future

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
  extends PlayMacroLogsImpl
  with IMCommonDi
  with AdvBuilderFactoryDi
  with IMItems
{

  import LOGGER._
  import mCommonDi._
  import slick.driver.api._


  /** Поиск id карточек, которые нужно глянуть на следующей стадии.
    * Запрос идёт вне транзакций, race-conditions будут учтены в последующем коде.
    * Главное требование: чтобы adId не повторялись в рамках одного потока. Проблем по идее быть не должно, но всё же.
    */
  def findAdIds(max: Int = MAX_ADS_PER_RUN): StreamingDBIO[Traversable[String], String]

  val now = DateTime.now


  /** Обработка одной карточки обычно тяжелая асинхронная операция,
    * которая может вызвать слишком резкие скачки нагрузки. Надо ограничивать аппетиты обработки.
    * Если уперлись в этот лимит, то будет повторный вызов run() (рекурсивно).
    */
  def MAX_ADS_PER_RUN: Int  = 10

  /** Делать передышку (прерывать обработку) в непрерывной обработке после этого числа пройденных карточек.
    * Если уперлись в этот лимит, то продолжение будет только после вызова run() извне.
    * Нужно в основном для защиты от нештатных ситуаций (бесконечный цикл, сильная долгая нагрузка на сеть/СУБД).
    */
  def MAX_ADS_PER_RUNS: Int = 500


  /** Основная метод запуска всего модуля на исполнение.
    *
    * @return Фьючерс с кол-вом отработанных карточек.
    */
  def run(): Future[Int] = {
    run(0)
  }

  /** 1. Запуск поиска id карточек, требующих обновления.
    * Функция также старается следить за расходование ресурсов системы, сглаживая возможную резкую нагрузку.
    *
    * @param counter Счетчик уже пройденных карточек.
    * @return Фьючерс с кол-вом отработанных карточек
    */
  def run(counter: Int): Future[Int] = {
    lazy val logPrefix = s"run($counter):"
    // У нас тут рекурсия, но надо защищаться от бесконечности. Ограничиваем счетчик вызовов run().
    if (counter > MAX_ADS_PER_RUNS) {
      warn(s"$logPrefix Too many ads for processing, lets stop it unconditionally. Something going wrong?")
      Future.successful(counter)

    } else {
      // запустить асинхронную подготовку общего контекста
      _builderCtxOuterFut

      for {
        adIds <- slick.db.run( findAdIds(MAX_ADS_PER_RUN) )

        ress  <- {
          Future.traverse(adIds) { adId =>
          runForAdId(adId)
            .map(_ => true)
            .recover { case ex: Throwable =>
              error(s"$logPrefix Failed to process ad[$adId]", ex)
              false
            }
          }
        }

        result <- {
          val countOk = ress.count(identity)
          val countFail = ress.count(!_)
          val count = countOk + countFail
          val counter2 = counter + count
          // Если было слишком много карточек за раз, то продолжить работу после небольшой паузы.
          if (count >= MAX_ADS_PER_RUN) {
            info(s"$logPrefix Done $count adv-items (failed=$countFail), but DB has more, lets run again...")
            run(counter2)

          } else {
            if (count > 0)
              info(s"$logPrefix Finished. $countOk ok, failed = $countFail. Total: $counter2")
            Future.successful(counter2)
          }
        }
      } yield {
        result
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
    * @param adId id обрабатываемой карточки.
    */
  def runForAdId(adId: String): Future[_] = {
    val madOptFut = mNodeCache.getById(adId)

    lazy val logPrefix = s"runForAdId($adId/${System.currentTimeMillis}):"
    trace(s"$logPrefix Starting...")

    // Нужны только item'ы, которые поддерживаются adv-билдерами
    val acc0Fut = for (madOpt <- madOptFut) yield {
      Acc(
        mad         = madOpt.get,
        ctxOuterFut = _builderCtxOuterFut
      )
    }
    val b0 = advBuilderFactory.builder(acc0Fut, now)

    // Собрать экшен для изменений в БД биллинга.
    val updateAction = for {

      // Читаем все обрабатываемые item'ы, попутно блокируя для апдейта ниже по транзакции.
      mitems <- findItemsForAdId(adId, b0.supportedItemTypes).forUpdate
      if hasItemsForProcessing(mitems)

      // Блокировка транзакции в недо-экшене, готовящем db-экшены и обновляющий карточку.
      acc2 <- {
        val fut = for {
          acc0 <- acc0Fut
          // Это сложная операция: выстраивание набора изменений, наложение его на карточку, сохранение карточки, выстраивание экшенов SQL-транзакции:
          tuData1 <- EsModelUtil.tryUpdate[MNode, TryUpdateBuilder]( MNode, TryUpdateBuilder(acc0) ) { tuData =>
            tryUpdateAd(tuData, mitems)
          }
        } yield {
          tuData1.acc
        }
        DBIO.from(fut)
      }

      // Внести накопленные изменения в БД биллинга
      _ <- DBIO.seq(acc2.dbActions: _*)

    } yield {
      trace(s"$logPrefix Done")
      acc2.mad
    }

    // Сохранить изменения во всех моделях, вернув итоговый фьючерс.
    val resFut = slick.db.run( updateAction.transactionally )

    // Залоггировать итоги работы:
    resFut.onFailure {
      // NSEE *обычно* значит, что hasItemsForProcessing() вернул false. Такое возможно, когда карточка была отработана одновременно с поиском.
      case ex: NoSuchElementException =>
        warn(s"$logPrefix Possibly no items for ad[$adId], but they expected to be moments ago. Race conditions?")
    }

    resFut
  }

}
