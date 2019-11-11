package util.cron

import akka.actor.{Cancellable, Scheduler}
import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImplLazy
import models.mcron.MCronTask
import models.mproj.ICommonDi
import play.api.inject.ApplicationLifecycle
import util.billing.cron.BillingCronTasks
import util.geo.IpGeoBaseImport
import util.img.cron.{PeriodicallyDeleteEmptyDirs, PeriodicallyDeleteNotExistingInPermanent}
import util.stat.StatCronTasks

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.13 11:42
 * Description: Запускалка периодических задач, некий cron, запускающий указанные функции по таймеру.
 *
 * Реализация происходит через akka scheduler и статический набор события расписания.
 * По мотивам http://stackoverflow.com/a/13469308
 */

// TODO Вынести cron в отдельный пакет, на крайняк в util или ещё куда-нибудь. Чтобы список модулей на вход получал через reference.conf.

class Crontab @Inject() (
  ipGeoBaseImport               : IpGeoBaseImport,
  statCronTasks                 : StatCronTasks,
  billingCronTasks              : BillingCronTasks,
  // images
  periodicallyDeleteEmptyDirs   : PeriodicallyDeleteEmptyDirs,
  periodicallyDeleteNotExistingInPermanent: PeriodicallyDeleteNotExistingInPermanent,
  // other
  lifecycle                     : ApplicationLifecycle,
  mCommonDi                     : ICommonDi
)
  extends MacroLogsImplLazy
{

  import mCommonDi._

  /** Список классов, которые являются поставщиками периодических задач при старте. */
  def TASK_PROVIDERS = Seq[ICronTasksProvider](
    billingCronTasks,
    ipGeoBaseImport,
    statCronTasks,
    periodicallyDeleteEmptyDirs,
    periodicallyDeleteNotExistingInPermanent
  )

  // Constructor -------------------------------
  private var _startedTimers = List.empty[Cancellable]

  // akka-2.5+: Чтобы избегать экзепшенов прямо в конструкторе, запуск таймеров скидываем в отдельный тред.
  for {
    ex <- {
      val fut = Future {
        _startedTimers = startTimers()
      }
      fut.failed
    }
  } {
    LOGGER.error("startTimers() totally failed! Crontab doesn't work at all.", ex)
  }


  // Destructor --------------------------------
  lifecycle.addStopHook { () =>
    Future {
      for (c <- _startedTimers) {
        try {
          c.cancel()
        } catch {
          case ex: Throwable =>
            LOGGER.warn(s"Cannot stop cron task $c", ex)
        }
      }
      LOGGER.trace(s"Stopped all ${_startedTimers.size} crontab tasks.")
      _startedTimers = Nil
    }
  }


  // API ---------------------------------------

  def sched: Scheduler = {
    try {
      actorSystem.scheduler
    } catch {
      // There is no started application
      case e: RuntimeException =>
        LOGGER.warn(s"${e.getClass.getSimpleName}: play-akka failed. Wait and retry... :: ${e.getMessage}", e)
        Thread.sleep(250)
        sched
    }
  }


  def startTimers(): List[Cancellable] = {
    val _sched = sched

    (for {
      clazz <- TASK_PROVIDERS.iterator
      task  <- clazz.cronTasks()
    } yield {
      LOGGER.trace(s"Adding cron task ${clazz.getClass.getSimpleName}/${task.displayName}: delay=${task.startDelay}, every=${task.every}")
      _sched.scheduleWithFixedDelay(task.startDelay, task.every) {
        new Runnable {
          override def run(): Unit = {
            try {
              LOGGER.trace(s"Executing task ${task.displayName}...")
              task.run()
            } catch {
              case ex: Throwable =>
                LOGGER.error(s"Cron task ${clazz.getClass.getSimpleName}/'${task.displayName}' failed to complete", ex)
            }
          }
        }
      }
    })
      .toList
  }

  def stopTimers(timers: Seq[Cancellable]): Unit =
    timers.foreach { _.cancel() }

}


/** Интерфейс для модулей, предоставляющих периодические задачи. */
trait ICronTasksProvider {

  /** Список задач, которые надо вызывать по таймеру. */
  def cronTasks(): Iterable[MCronTask]

}
