package util

import play.api.Play.current
import akka.actor.{Scheduler, Cancellable}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import models.MPictureTmp
import play.api.Logger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.13 11:42
 * Description: Выполнялка периодических задач, таких как постоянное инфляция популярности кампаний.
 *
 * Реализация происходит через akka scheduler и статический набор события расписания.
 * По мотивам http://stackoverflow.com/a/13469308
 */

object Crontab {

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
      schedule(10 seconds, 5 minutes) { MPictureTmp.cleanupOld() }
    )
  }

  def stopTimers(timers: Seq[Cancellable]) {
    timers.foreach { _.cancel() }
  }

}
