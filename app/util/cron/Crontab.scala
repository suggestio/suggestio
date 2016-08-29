package util.cron

import akka.actor.{Cancellable, Scheduler}
import com.google.inject.Inject
import models.mcron.ICronTask
import models.mproj.ICommonDi
import play.api.inject.ApplicationLifecycle
import util.PlayLazyMacroLogsImpl
import util.billing.cron.BillingCronTasks
import util.geo.IpGeoBaseImport
import util.health.AdnGeoParentsHealth
import util.img.cron.{PeriodicallyDeleteEmptyDirs, PeriodicallyDeleteNotExistingInPermanent}

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
trait ICrontab

class Crontab @Inject() (
  // geo-nodes
  geoParentsHealth              : AdnGeoParentsHealth,
  // geoip
  ipGeoBaseImport               : IpGeoBaseImport,
  // billing
  billingCronTasks              : BillingCronTasks,
  // images
  periodicallyDeleteEmptyDirs   : PeriodicallyDeleteEmptyDirs,
  periodicallyDeleteNotExistingInPermanent: PeriodicallyDeleteNotExistingInPermanent,
  // other
  lifecycle                     : ApplicationLifecycle,
  mCommonDi                     : ICommonDi
)
  extends PlayLazyMacroLogsImpl
  with ICrontab
{

  import LOGGER._
  import mCommonDi._

  /** Список классов, которые являются поставщиками периодических задач при старте. */
  def TASK_PROVIDERS = Seq[ICronTasksProvider](
    billingCronTasks, ipGeoBaseImport, geoParentsHealth,
    periodicallyDeleteEmptyDirs, periodicallyDeleteNotExistingInPermanent
  )

  // Constructor -------------------------------
  private var _startedTimers = startTimers()

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
