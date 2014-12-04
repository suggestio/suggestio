package io.suggest.util

import scala.concurrent.{Future, ExecutionContext}
import io.suggest.model._
import org.elasticsearch.client.Client
import scala.util.{Failure, Success}
import org.elasticsearch.indices.IndexMissingException

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.13 14:51
 * Description: Утиль для управления виртуальными индексами через соответствующие модели. Расчитано на работу извне flow.
 */
object VirtualIndexUtil extends MacroLogsImpl {

  import LOGGER._

  /**
   * Если у индексов для домена есть несколько поколений, то откатить поколения на самые старые: удалить новые индексы
   * и выставить MDVISearchPtr.
   * Функция полностью асинхронна.
   * @return Фьючерс для синхронизации.
   */
  def downgradeAll(implicit ec:ExecutionContext, client:Client): Future[_] = {
    val logPrefix = "downgradeAll():"
    trace(logPrefix + " starting...")
    MVIUnit.getAllUnits.flatMap { mvis =>
      val rowkeyInxGroups = mvis
        .groupBy(_.getRowKeyStr)
        .mapValues {
          _.sortBy(_.generation)
        }
      // Параллельно делаем downgrade индексов по доменам
      val fut = Future.traverse(rowkeyInxGroups) {
        // Нечего даунгрейдить, если 0 или 1 индекс всего лишь.
        case (rowKey, mviGroup) if mviGroup.isEmpty || mviGroup.tail.isEmpty =>
          trace(s"$logPrefix Nothing to downgrade on rowKey=$rowKey indices=${mdvisAsString(mviGroup)}")
          Future successful None

        // Есть чего поудалять.
        case (rowKey, mviGroup) =>
          val restInx = mviGroup.head
          val toRmInxs = mviGroup.tail
          trace(s"$logPrefix Downgrading indices for rowKey=$rowKey to index=${restInx.vin}. Drop indices (${toRmInxs.size}): ${mdvisAsString(toRmInxs)}")
          Future.traverse(toRmInxs) { rmMvi =>
            rmMvi.eraseBackingIndex.recover {
              case ex: IndexMissingException =>
                debug(s"$logPrefix Ignoring ${ex.getClass.getSimpleName}: Already deleted index ${rmMvi.toShortString} :: ${ex.getMessage}")
                true
              case ex: UnsupportedOperationException =>
                debug(s"$logPrefix ${ex.getClass.getSimpleName} for index ${rmMvi.toShortString}")
                true
            } flatMap {
              _ => rmMvi.delete
            }
          }
      }
      fut onComplete {
        case Success(_)  => info(logPrefix + " finished")
        case Failure(ex) => error(logPrefix + " failed", ex)
      }
      fut
    }
  }

  private def mdvisAsString(indices: Seq[MVIUnit]): String = {
    indices.map(_.vin).mkString(",")
  }

}
