package util

import com.google.inject.Inject
import models.im.MLocalImg
import models.mcron.ICronTask
import play.api.Play.current
import akka.actor.{Scheduler, Cancellable}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import util.geo.IpGeoBaseImport
import play.api.Application
import util.billing.{MmpCronTasks, Billing}
import util.health.AdnGeoParentsHealth

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.13 11:42
 * Description: Запускалка периодических задач, некий cron, запускающий указанные функции по таймеру.
 *
 * Реализация происходит через akka scheduler и статический набор события расписания.
 * По мотивам http://stackoverflow.com/a/13469308
 */

class Crontab @Inject() (
  geoParentsHealth              : AdnGeoParentsHealth,
  ipGeoBaseImport               : IpGeoBaseImport,
  billing                       : Billing,
  mmpCronTasks                  : MmpCronTasks
)
  extends PlayLazyMacroLogsImpl
{

  import LOGGER._

  /** Список классов, которые являются поставщиками периодических задач при старте. */
  def TASK_PROVIDERS = Iterator[ICronTasksProvider](
    billing, mmpCronTasks, ipGeoBaseImport, MLocalImg, geoParentsHealth
  )

  def sched: Scheduler = {
    try
      Akka.system.scheduler
    catch {
      // There is no started application
      case e: RuntimeException =>
        warn(s"${e.getClass.getSimpleName}: play-akka failed. Wait and retry... :: ${e.getMessage}", e)
        Thread.sleep(250)
        sched
    }
  }


  def startTimers(app: Application): List[Cancellable] = {
    val _sched = sched

    val iter = for {
      clazz <- TASK_PROVIDERS
      task  <- clazz.cronTasks(app)
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
  def cronTasks(app: Application): TraversableOnce[ICronTask]
}


/** При использование stackable trait и abstract override имеет смысл подмешивать этот трейт с дефолтовой пустой реализацией. */
trait CronTasksProviderEmpty extends ICronTasksProvider {
  override def cronTasks(app: Application): TraversableOnce[ICronTask] = Nil
}
