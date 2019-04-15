package io.suggest.stat.saver

import javax.inject.{Inject, Singleton}
import io.suggest.util.logs.MacroLogsDyn
import play.api.Configuration
import play.api.inject.{ApplicationLifecycle, Injector}

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
                                injector      : Injector,
                              )
  extends MacroLogsDyn
{

  injector.instanceOf[ApplicationLifecycle].addStopHook { () =>
    BACKEND.close()
  }

  private def _bulk  = injector.instanceOf[BulkProcessorSaveBackend]
  private def _plain = injector.instanceOf[PlainSaverBackend]
  private def _dummy = injector.instanceOf[DummySaverBackend]

  private def defaultBackend: StatSaverBackend = _bulk

  /** Используемый backend для сохранения статистики. */
  val BACKEND: StatSaverBackend = {
    val ck = "sc.stat.saver.type"
    injector
      .instanceOf[Configuration]
      .getOptional[String](ck)
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
