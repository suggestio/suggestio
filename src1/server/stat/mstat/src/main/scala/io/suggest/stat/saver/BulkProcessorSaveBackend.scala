package io.suggest.stat.saver

import com.google.inject.{Inject, Singleton}
import io.suggest.async.AsyncUtil
import io.suggest.es.model.IEsModelDiVal
import io.suggest.stat.m.{MStat, MStats}
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.action.bulk.{BulkProcessor, BulkRequest, BulkResponse}
import org.elasticsearch.common.unit.{ByteSizeValue, TimeValue}

import scala.concurrent.Future

/** BulkProcessor backend накапливает очередь и отправляет всё индексацию разом. */
@Singleton
class BulkProcessorSaveBackend @Inject() (
  mStats                  : MStats,
  asyncUtil               : AsyncUtil,
  mCommonDi               : IEsModelDiVal
)
  extends StatSaverBackend
  with MacroLogsImpl
{

  import mCommonDi._
  import LOGGER._

  /** Не хранить в очереди дольше указанного интервала (в секундах). */
  def FLUSH_INTERVAL_SECONDS: Long = {
    //configuration.getOptional[Long]("sc.stat.saver.bp.flush.interval.seconds").getOrElse(20L)
    20L
  }

  /** Максимальный размер bulk-реквеста в байтах. */
  def BULK_SIZE_BYTES: Long = {
    //configuration.getOptional[Long]("sc.stat.saver.bp.size.bytes").getOrElse(200000L)
    200000L
  }

  /** Используемый bulk processor. */
  protected val bp: BulkProcessor = {
    val br = BulkProcessor.builder(esClient, new BulkProcessor.Listener {
      override def beforeBulk(executionId: Long, request: BulkRequest): Unit = {
        trace(s"[$executionId] Will bulk save stats with ${request.numberOfActions} actions")
      }

      override def afterBulk(executionId: Long, request: BulkRequest, response: BulkResponse): Unit = {}

      override def afterBulk(executionId: Long, request: BulkRequest, failure: Throwable): Unit = {
        error(s"[$executionId] error occured while bulk-saving ${request.numberOfActions} actions", failure)
      }
    })
    br.setName("statSaver")
      .setFlushInterval( TimeValue.timeValueSeconds(FLUSH_INTERVAL_SECONDS) )
      .setBulkSize(new ByteSizeValue(BULK_SIZE_BYTES))
      .build()
  }

  private def _asyncSafe[T](f: => T): Future[T] = {
    Future(f)(asyncUtil.singleThreadCpuContext)
  }

  override def save(stat: MStat): Future[_] = {
    // Подавляем блокировку синхронизации в bp через отдельный execution context с очередью задач.
    _asyncSafe {
      val irb = mStats.prepareIndex(stat).request()
      bp.add(irb)
      None    // Не возвращать инстанс BP наружу, пусть лучше будет какой-нибудь мусор.
    }
  }

  override def flush(): Unit = {
    bp.flush()
  }

  override def close(): Future[_] = {
    _asyncSafe {
      bp.close()
    }
  }

}
