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
object VirtualIndexUtil {

  private val LOGGER = new LogsImpl(getClass)
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
    MDVIActive.getAll.flatMap { mdviActives =>
      val dkeyInxGroups = mdviActives
        .groupBy(_.getDkey)
        .mapValues {
          _.sortBy(_.getGeneration)
        }
      // Параллельно делаем downgrade индексов по доменам
      val fut = Future.traverse(dkeyInxGroups) {
        // Нечего даунгрейдить, если 0 или 1 индекс всего лишь.
        case (dkey, mdviGroup) if mdviGroup.isEmpty || mdviGroup.tail.isEmpty =>
          trace(s"$logPrefix Nothing to downgrade on dkey=$dkey indices=${mdvisAsString(mdviGroup)}")
          Future.successful(())

        // Есть чего поудалять.
        case (dkey, mdviGroup) =>
          val restInx = mdviGroup.head
          val toRmInxs = mdviGroup.tail
          trace(s"$logPrefix Downgrading indices for dkey=$dkey to index=${restInx.getVin}. Drop indices (${toRmInxs.size}): ${mdvisAsString(toRmInxs)}")
          // Сначала обновить inx ptr
          val searchPtr = new MDVISearchPtr(dkey, List(restInx.getVin))
          searchPtr.save.flatMap { _ =>
            Future.traverse(toRmInxs) { rmMdviActive =>
              rmMdviActive
                .eraseBackingIndex.recover {
                  case ex: IndexMissingException =>
                    debug(s"$logPrefix Ignoring ${ex.getClass.getSimpleName} while deleting index ${searchPtr.vins.head} :: ${ex.getMessage}")
                    true
                } flatMap {
                  _ => rmMdviActive.delete
                }
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

  private def mdvisAsString(indices: Seq[MDVIActive]): String = {
    indices.map(_.getVin).mkString(",")
  }

}
