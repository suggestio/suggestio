package util.billing.cron

import io.suggest.mbill2.m.item.{IMItems, MAdItemIds, MItem}
import io.suggest.model.es.EsModelUtil
import models._
import models.adv.build.{Acc, TryUpdateBuilder}
import models.mproj.IMCommonDi
import org.joda.time.DateTime
import util.PlayMacroLogsImpl
import util.adv.build.{AdvBuilderFactoryDi, IAdvBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.15 22:01
 * Description: Общий код периодической проверки и обработки очереди размещений.
 *
 * Код вывода в выдачу и последующего сокрытия рекламных карточек крайне похож,
 * поэтому он вынесен в трейт.
 */
abstract class AdvsUpdate
  extends PlayMacroLogsImpl
  with IMCommonDi
  with AdvBuilderFactoryDi
  with IMItems
{

  import LOGGER._
  import mCommonDi._
  import dbConfig.driver.api._

  /** Поиск данных для обработки. */
  // TODO Хотеть DBIOAction[_, Streamable, Effect.Read], но голый SQL-запрос в модели мешает этому.
  def findItemsForProcessing: DBIOAction[Traversable[MAdItemIds], NoStream, Effect.Read]

  lazy val now = DateTime.now()

  /** Запуск обработки нового биллинга на исполнение. */
  def run(): Unit = {
    /*
    TODO Стриминг будет возможен, если MItems научится возвращаться Streamable.
    dbConfig.db
      .stream( findItemsForProcessing )
      .foreach(runForAd)
    */
    for {
      datas <- dbConfig.db.run( findItemsForProcessing )
    } yield {
      datas.foreach(runForAd)
    }
  }

  protected def _buildAction(b: IAdvBuilder, mitem: MItem): IAdvBuilder

  /** Запуск обработки item'ов для одной рекламной карточки. */
  def runForAd(d: MAdItemIds): Unit = {
    val madOptFut = mNodeCache.getById(d.adId)

    lazy val logPrefix = s"runForAd(${d.adId}/${System.currentTimeMillis}):"
    trace(s"$logPrefix ${d.itemIds.mkString(",")}")

    // Нужны только item'ы, которые поддерживаются adv-билдерами
    val acc0Fut = for (madOpt <- madOptFut) yield {
      Acc(madOpt.get)
    }
    val b0 = advBuilderFactory.builder(acc0Fut)
    val tuData0Fut = for (acc0 <- acc0Fut) yield {
      TryUpdateBuilder(acc0)
    }
    val supportedItypes = b0.supportedItemTypes.toSet

    // Собрать экшен для изменений в БД биллинга.
    val updateAction = for {
      // Читаем все обрабатываемые item'ы,
      mitems <- mItems.getByIds(d.itemIds).forUpdate
      // Чисто самоконтроль, иначе и быть не может, но всё же... Убедиться, что id карточки релевантен каждому item'у.
      if mitems.forall(_.adId == d.adId)

      // Необходимо выкинуть item'ы, не поддерживаемые adv-bilder'ом
      mitems2 = mitems.filter { i =>
        supportedItypes.contains(i.iType)
      }
      if mitems2.nonEmpty

      // Блокировка транзакции в недо-экшене, готовящем db-экшены и обновляющий карточку.
      acc2 <- {
        val fut = for {
          tuData0 <- tuData0Fut
          // Это сложная операция: выстраивание набора изменений, наложение его на карточку, сохранение карточки, выстраивание экшенов SQL-транзакции:
          tuData1 <- EsModelUtil.tryUpdate[MNode, TryUpdateBuilder](tuData0) { tuData =>
            val b2 = mitems2.foldLeft(b0)(_buildAction)
            for (acc2 <- b2.accFut) yield {
              TryUpdateBuilder(acc2)
            }
          }
        } yield {
          tuData1.acc
        }
        DBIO.from(fut)
      }

      // Внести накопленные изменения в БД биллинга
      _ <- DBIO.seq(acc2.dbActions: _*)

    } yield {
      (mitems2, acc2.mad)
    }

    // Сохранить изменения в БД биллинга, вернув итоговый фьючерс.
    dbConfig.db.run( updateAction.transactionally )
  }

}
