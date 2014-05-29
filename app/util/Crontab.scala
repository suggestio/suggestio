package util

import play.api.Play.current
import akka.actor.{Scheduler, Cancellable}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import models.MPictureTmp
import play.api.Logger
import util.billing.{MmpDailyBilling, Billing}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.13 11:42
 * Description: Выполнялка периодических задач, таких как постоянное инфляция популярности кампаний.
 *
 * Реализация происходит через akka scheduler и статический набор события расписания.
 * По мотивам http://stackoverflow.com/a/13469308
 */

object Crontab extends PlayMacroLogsImpl {

  import LOGGER._

  def sched: Scheduler = {
    try
      Akka.system.scheduler
    catch {
      // There is no started application
      case e: RuntimeException =>
        Logger(getClass).warn(s"${e.getClass.getSimpleName}: play-akka failed. Wait and retry... :: ${e.getMessage}", e)
        Thread.sleep(250)
        sched
    }
  }

  def startTimers: List[Cancellable] = {
    val _sched = sched
    import _sched.schedule
    List(
      // Чистить tmp-картинки
      schedule(7 seconds, 5 minutes) {
        try {
          MPictureTmp.cleanupOld()
        } catch {
          case ex: Throwable => error(s"Cron: MPictureTmp.cleanupOld() failed", ex)
        }
      },
      // Производить начисление абон.платы.
      schedule(10 seconds, Billing.SCHED_TARIFFICATION_DURATION) {
        try {
          Billing.processFeeTarificationAll()
        } catch {
          case ex: Throwable => error("Cron: Billing:processFeeTarificationAll() failed", ex)
        }
      },
      // Автоматически аппрувить залежавшиеся в очереди реквесты.
      schedule(5 seconds, 2 minutes) {
        try {
          MmpDailyBilling.autoApplyOldAdvReqs()
        } catch {
          case ex: Throwable => error("Cron: MmpDailyBilling.autoApplyOldAdvReqs() failed", ex)
        }
      },
      // Отправлять в выдачу карточки, время которых уже настало.
      schedule(15 seconds, 2 minutes) {
        try {
          MmpDailyBilling.advertiseOfflineAds()
        } catch {
          case ex: Throwable => error("Cron: MmpDailyBilling.advertiseOfflineAds() failed", ex)
        }
      }
    )

  }

  def stopTimers(timers: Seq[Cancellable]) {
    timers.foreach { _.cancel() }
  }

}
