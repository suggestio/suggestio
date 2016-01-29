package util.billing

import com.google.inject.Inject
import models.adv.MAdvReq
import models.mcron.{MCronTask, ICronTask}
import models.mproj.ICommonDi
import org.joda.time.Period
import play.api.db.Database
import play.api.{Application, Configuration}
import util.{PlayMacroLogsImpl, ICronTasksProvider}
import util.async.AsyncUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.11.15 11:16
 * Description: Провайдер задач для Cron'а. Задачи связаны с обслуживанием биллинга на
 * посуточных тарифах размещения.
 *
 * Поддержка высокоуровневых периодических задач вынесена из [[MmpDailyBilling]], чтобы
 * решить проблемы с циклическими зависямостями между [[MmpDailyBilling]] и [[util.adv.AdvUtil]].
 */
class MmpCronTasks @Inject() (
  mmpDailyBilling         : MmpDailyBilling,
  aoaFac                  : AdvertiseOfflineAdvsFactory,
  deaFac                  : DepublishExpiredAdvsFactory,
  mCommonDi               : ICommonDi
)
  extends ICronTasksProvider
  with PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Включен ли биллинг по крону? Будет выполнятся публикация карточек, их сокрытие и т.д. */
  def CRON_BILLING_CHECK_ENABLED: Boolean = {
    configuration.getBoolean("mmp.daily.check.enabled")
      .getOrElse(true)
  }

  /** Как часто надо проверять таблицу advsOK на предмет необходимости изменений в выдаче. */
  def CHECK_ADVS_OK_DURATION: FiniteDuration = {
    configuration.getInt("mmp.daily.check.advs.ok.every.seconds")
      .getOrElse(120)
      .seconds
  }

  /** Не раньше какого времени можно запускать auto-accept. */
  val AUTO_ACCEPT_REQS_AFTER_HOURS = configuration.getInt("mmp.daily.accept.auto.after.hours") getOrElse 16

  override def cronTasks(app: Application): TraversableOnce[ICronTask] = {
    val enabled = CRON_BILLING_CHECK_ENABLED
    info("enabled = " + enabled)
    if (enabled) {
      val every = CHECK_ADVS_OK_DURATION

      val applyOldReqs = MCronTask(
        startDelay = 3.seconds,
        every = every,
        displayName = "autoApplyOldAdvReqs()"
      ) {
        autoApplyOldAdvReqs()
      }

      val depubExpired = MCronTask(
        startDelay = 10.seconds,
        every = every,
        displayName = "depublishExpiredAdvs()"
      ) {
        deaFac.create().run()
      }

      val advOfflineAdvs = MCronTask(
        startDelay = 30.seconds,
        every = every,
        displayName = "advertiseOfflineAds()"
      ) {
        aoaFac.create().run()
      }
      List(applyOldReqs, depubExpired, advOfflineAdvs)

    } else {
      Nil
    }
  }

  /** Цикл автоматического накатывания MAdvReq в MAdvOk. Нужно найти висячие MAdvReq и заапрувить их. */
  def autoApplyOldAdvReqs() {
    val period = new Period(AUTO_ACCEPT_REQS_AFTER_HOURS, 0, 0, 0)
    // Найти запросы размещения, чей срок уже пришел
    val advsReqFut = Future {
      db.withConnection { implicit c =>
        MAdvReq.findCreatedLast(period)
      }
    }(AsyncUtil.jdbcExecutionContext)
    // Обработать найденные запросы размещения.
    for (advsReq <- advsReqFut) {
      for (mar <- advsReq) {
        mmpDailyBilling.acceptAdvReq(mar, isAuto = true)
      }
    }
  }

}
