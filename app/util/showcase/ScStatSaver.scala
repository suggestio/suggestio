package util.showcase

import com.google.inject.{Inject, Singleton}
import io.suggest.async.AsyncUtil
import io.suggest.ym.model.stat.{MAdStat, MAdStats}
import models.mproj.ICommonDi
import org.elasticsearch.action.bulk.{BulkProcessor, BulkRequest, BulkResponse}
import org.elasticsearch.common.unit.{ByteSizeValue, TimeValue}
import play.api.inject.ApplicationLifecycle
import util.{PlayMacroLogsDyn, PlayMacroLogsImpl}

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.10.14 9:47
 * Description: Сохранялка статистики.
 * Возникла из-за приближающейся необходимости снизить нагрузку на ES из-за сохранения статистики.
 *
 * 2014.oct.17: Простая реализация через BulkProcessor.
 * Она имеет внутренние блокировки, подавляемые через содержание отдельного однопоточного thread-pool'a.
 * TODO Надо бы сделать через akka actor.
 */
@Singleton
class ScStatSaver @Inject() (
  lifecycle               : ApplicationLifecycle,
  mCommonDi               : ICommonDi
)
  extends PlayMacroLogsDyn
{

  import mCommonDi.{current, configuration}

  lifecycle.addStopHook { () =>
    BACKEND.close()
  }

  private def _inject[T <: ScStatSaverBackend : ClassTag]: T = {
    current.injector.instanceOf[T]
  }

  private def _bulk  = _inject[BulkProcessorSaveBackend]
  private def _plain = _inject[PlainSaverBackend]
  private def _dummy = _inject[DummySaverBackend]

  private def defaultBackend: ScStatSaverBackend = _bulk

  /** Используемый backend для сохранения статистики. */
  val BACKEND: ScStatSaverBackend = {
    val ck = "sc.stat.saver.type"
    configuration.getString(ck)
      .fold [ScStatSaverBackend] (defaultBackend) { raw =>
        raw.trim.toLowerCase match {
          case "plain" | ""     =>
            _plain
          case "bp"    | "bulk" =>
            _bulk
          case "dummy" | "null" =>
            LOGGER.warn("BACKEND: dummy save backend enabled. All stats will be saved to /dev/null!")
            _dummy
          case other =>
            val backend = defaultBackend
            LOGGER.warn(s"BACKEND: Unknown value '$other' for conf key '$ck'. Please check your application.conf. Fallbacking to default backend: ${backend.getClass.getSimpleName}")
            backend
        }
      }
  }

}


/** Интерфейс для backend'ов сохранения статистики. */
trait ScStatSaverBackend {
  /** Сохранение. Бэкэнд может отправлять в свою очередь или в хранилище. */
  def save(stat: MAdStat): Future[_]

  /** Сброс накопленной очереди, если такая имеется. */
  def flush(): Unit

  /** Завершение работы backend'a. */
  def close(): Future[_]
}


/** Бэкэнд сохранения статистики, который сохраняет всё в /dev/null. */
class DummySaverBackend extends ScStatSaverBackend {
  /** Сохранение. Бэкэнд может отправлять в свою очередь или в хранилище. */
  override def save(stat: MAdStat): Future[_] = {
    Future.successful(Nil)
  }

  /** Сброс накопленной очереди, если такая имеется. */
  override def flush(): Unit = {}

  /** Завершение работы backend'a. */
  override def close(): Future[_] = {
    Future.successful(None)
  }
}


/** Plain backend вызывает save() для всех элементов очереди. */
class PlainSaverBackend @Inject() (
  mAdStats                      : MAdStats
)
  extends ScStatSaverBackend
{
  override def save(stat: MAdStat): Future[_] = {
    mAdStats.save(stat)
  }
  override def flush(): Unit = {}
  override def close(): Future[_] = {
    Future.successful(None)
  }
}


/** BulkProcessor backend накапливает очередь и отправляет всё индексацию разом. */
@Singleton
class BulkProcessorSaveBackend @Inject() (
  mAdStats                : MAdStats,
  asyncUtil               : AsyncUtil,
  mCommonDi               : ICommonDi
)
  extends ScStatSaverBackend
  with PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Не хранить в очереди дольше указанного интервала (в секундах). */
  def FLUSH_INTERVAL_SECONDS: Long = {
    configuration.getLong("sc.stat.saver.bp.flush.interval.seconds").getOrElse(20L)
  }

  /** Максимальный размер bulk-реквеста в байтах. */
  def BULK_SIZE_BYTES: Long = {
    configuration.getLong("sc.stat.saver.bp.size.bytes").getOrElse(200000L)
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

  override def save(stat: MAdStat): Future[_] = {
    // Подавляем блокировку синхронизации в bp через отдельный execution context с очередью задач.
    _asyncSafe {
      val irb = mAdStats.prepareIndex(stat).request()
      bp.add(irb)
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

