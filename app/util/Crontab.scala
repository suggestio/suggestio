package util

import models.im.MLocalImg
import play.api.Play.current
import akka.actor.{Scheduler, Cancellable}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import util.geo.IpGeoBaseImport
import models.ICronTask
import play.api.Application
import util.billing.{MmpDailyBilling, Billing}
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

class Crontab extends PlayLazyMacroLogsImpl {

  import LOGGER._

  /** Список классов, которые являются поставщиками периодических задач при старте. */
  def TASK_PROVIDERS = List[CronTasksProvider](
    Billing, MmpDailyBilling, IpGeoBaseImport, MLocalImg, AdnGeoParentsHealth
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
    TASK_PROVIDERS
      .iterator
      .flatMap { clazz =>
        clazz.cronTasks(app).toIterator.map { cronTask =>
          _sched.schedule(cronTask.startDelay, cronTask.every) {
            try {
              cronTask.run()
            } catch {
              case ex: Throwable => error(s"Cron task ${clazz.getClass.getSimpleName}/'${cronTask.displayName}' failed to complete", ex)
            }
          }
        }
      }
      .toList
  }

  def stopTimers(timers: Seq[Cancellable]) {
    timers.foreach { _.cancel() }
  }

}


/** Интерфейс для модулей, предоставляющих периодические задачи. */
trait CronTasksProvider {

  /** Список задач, которые надо вызывать по таймеру. */
  def cronTasks(app: Application): TraversableOnce[ICronTask]
}


/** При использование stackable trait и abstract override имеет смысл подмешивать этот трейт с дефолтовой пустой реализацией. */
trait CronTasksProviderEmpty extends CronTasksProvider {
  override def cronTasks(app: Application): TraversableOnce[ICronTask] = Nil
}
