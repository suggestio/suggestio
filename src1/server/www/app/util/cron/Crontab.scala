package util.cron

import akka.actor.{Cancellable, Scheduler}
import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImplLazy
import models.mcron.ICronTask
import models.mproj.ICommonDi
import play.api.inject.ApplicationLifecycle
import util.billing.cron.BillingCronTasks
import util.geo.IpGeoBaseImport
import util.health.AdnGeoParentsHealth
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
  geoParentsHealth              : AdnGeoParentsHealth,
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

  import LOGGER._
  import mCommonDi._

  /** Список классов, которые являются поставщиками периодических задач при старте. */
  def TASK_PROVIDERS = Seq[ICronTasksProvider](
    billingCronTasks,
    ipGeoBaseImport,
    statCronTasks,
    geoParentsHealth,
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
            warn(s"Cannot stop cron task $c", ex)
        }
      }
      trace(s"Stopped all ${_startedTimers.size} crontab tasks.")
      _startedTimers = Nil
    }
  }


  // API ---------------------------------------

  def sched: Scheduler = {
    try
      actorSystem.scheduler
    catch {
      // There is no started application
      case e: RuntimeException =>
        warn(s"${e.getClass.getSimpleName}: play-akka failed. Wait and retry... :: ${e.getMessage}", e)
        Thread.sleep(250)
        sched
    }
  }


  def startTimers(): List[Cancellable] = {
    val _sched = sched

    val iter = for {
      clazz <- TASK_PROVIDERS.iterator
      task  <- clazz.cronTasks()
    } yield {
      trace(s"Adding cron task ${clazz.getClass.getSimpleName}/${task.displayName}: delay=${task.startDelay}, every=${task.every}")
      _sched.schedule(task.startDelay, task.every) {
        try {
          trace(s"Executing task ${task.displayName}...")
          task.run()
        } catch {
          case ex: Throwable =>
            error(s"Cron task ${clazz.getClass.getSimpleName}/'${task.displayName}' failed to complete", ex)
        }
      }
    }

    iter.toList
  }

  def stopTimers(timers: Seq[Cancellable]) {
    timers.foreach { _.cancel() }
  }

}


/** Интерфейс для модулей, предоставляющих периодические задачи. */
trait ICronTasksProvider {

  /** Список задач, которые надо вызывать по таймеру. */
  def cronTasks(): TraversableOnce[ICronTask]
}


/** При использование stackable trait и abstract override имеет смысл подмешивать этот трейт с дефолтовой пустой реализацией. */
trait CronTasksProviderEmpty extends ICronTasksProvider {
  override def cronTasks(): TraversableOnce[ICronTask] = Nil
}
