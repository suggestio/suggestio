package io.suggest.stat.saver

import com.google.inject.{Inject, Singleton}
import io.suggest.model.es.IEsModelDiVal
import io.suggest.util.MacroLogsDyn
import play.api.inject.ApplicationLifecycle

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
class PlayStatSaver @Inject() (
  lifecycle               : ApplicationLifecycle,
  mCommonDi               : IEsModelDiVal
)
  extends MacroLogsDyn
{

  import mCommonDi._

  lifecycle.addStopHook { () =>
    BACKEND.close()
  }

  private def _inject[T <: StatSaverBackend : ClassTag]: T = {
    current.injector.instanceOf[T]
  }

  private def _bulk  = _inject[BulkProcessorSaveBackend]
  private def _plain = _inject[PlainSaverBackend]
  private def _dummy = _inject[DummySaverBackend]

  private def defaultBackend: StatSaverBackend = _bulk

  /** Используемый backend для сохранения статистики. */
  val BACKEND: StatSaverBackend = {
    val ck = "sc.stat.saver.type"
    configuration.getString(ck)
      .fold [StatSaverBackend] (defaultBackend) { raw =>
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
