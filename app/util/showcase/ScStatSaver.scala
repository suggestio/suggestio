package util.showcase

import io.suggest.ym.model.stat.MAdStat
import org.elasticsearch.action.bulk.{BulkResponse, BulkRequest, BulkProcessor}
import org.elasticsearch.common.unit.{ByteSizeValue, TimeValue}
import play.api.Logger
import util.{EcParInfo, AsyncUtil, PlayMacroLogsImpl}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.10.14 9:47
 * Description: Сохранялка статистики. Возникла из-за приближающейся необходимости снизить нагрузку на ES из-за
 * сохранения статистики.
 * 2014.oct.17: Простая реализация через BulkProcessor. Она имеет внутренние блокировки, подавляемые через содержание
 * отдельного однопоточного thread-pool'a.
 * TODO Надо бы сделать через akka actor.
 */
object ScStatSaver {

  private def LOGGER = Logger(getClass)

  private def defaultBackend = new BulkProcessorSaveBackend

  /** Используемый backend для сохранения статистики. */
  val BACKEND: ScStatSaverBackend = {
    val ck = "sc.stat.saver.type"
    configuration.getString(ck)
      .fold [ScStatSaverBackend] (defaultBackend) { raw =>
        raw.trim.toLowerCase match {
          case "plain" | ""     => new PlainSaverBackend
          case "bp"    | "bulk" => new BulkProcessorSaveBackend
          case "dummy" | "null" =>
            LOGGER.warn("BACKEND: dummy save backend enabled. All stats will be saved to /dev/null!")
            new DummySaverBackend
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
  def close(): Unit
}


/** Бэкэнд сохранения статистики, который сохраняет всё в /dev/null. */
class DummySaverBackend extends ScStatSaverBackend {
  /** Сохранение. Бэкэнд может отправлять в свою очередь или в хранилище. */
  override def save(stat: MAdStat): Future[_] = {
    Future successful Nil
  }

  /** Сброс накопленной очереди, если такая имеется. */
  override def flush(): Unit = {}

  /** Завершение работы backend'a. */
  override def close(): Unit = {}
}


/** Plain backend вызывает save() для всех элементов очереди. */
class PlainSaverBackend extends ScStatSaverBackend {
  override def save(stat: MAdStat): Future[_] = {
    stat.save
  }
  override def flush(): Unit = {}
  override def close(): Unit = {}
}


/** BulkProcessor backend накапливает очередь и отправляет всё индексацию разом. */
class BulkProcessorSaveBackend extends ScStatSaverBackend with PlayMacroLogsImpl {

  /** Не хранить в очереди дольше указанного интервала (в секундах). */
  def FLUSH_INTERVAL_SECONDS: Long = configuration.getLong("sc.stat.saver.bp.flush.interval.seconds").getOrElse(20L)

  /** Максимальный размер bulk-реквеста в байтах. */
  def BULK_SIZE_BYTES: Long = configuration.getLong("sc.stat.saver.bp.size.bytes").getOrElse(200000L)

  import LOGGER._

  /** Используемый bulk processor. */
  protected val bp: BulkProcessor = {
    val br = BulkProcessor.builder(client, new BulkProcessor.Listener {
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

  /** ExecutionContext. При добавлении элементов в BulkProcessor наступает полная синхронизация,
    * поэтому нет смысла держать больше одного потока. */
  protected val ec = AsyncUtil.mkEc("sc.stat.saver.bp.ec", EcParInfo(1.0F, 1))

  override def save(stat: MAdStat): Future[_] = {
    // Подавляем блокировку синхронизации в bp через отдельный execution context с очередью задач.
    Future {
      bp add stat.prepareIndex.request()
    }(AsyncUtil.singleThreadCpuContext)
  }

  override def flush(): Unit = {
    bp.flush()
  }

  override def close(): Unit = {
    bp.close()
  }

}

