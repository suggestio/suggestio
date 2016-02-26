package util.billing.cron

import com.google.inject.Inject
import models.mcron.{ICronTask, MCronTask}
import models.mproj.ICommonDi
import play.api.Application
import util.adv.direct.AdvDirectBilling
import util.{ICronTasksProvider, PlayMacroLogsImpl}

import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.11.15 11:16
 * Description: Провайдер задач Биллинга v2+ для Cron'а.
 * Т.к. все товары и услуги были унифицированы через MItem, то их обслуживание так-же потребовало унификации.
 */
class BillingCronTasks @Inject()(
  advDirectBilling        : AdvDirectBilling,
  aoaFac                  : ActivateOfflineAdvsFactory,
  deaFac                  : DisableExpiredAdvsFactory,
  mCommonDi               : ICommonDi
)
  extends ICronTasksProvider
  with PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Включен ли биллинг по крону? Будет выполнятся публикация карточек, их сокрытие и т.д. */
  private def CRON_BILLING_CHECK_ENABLED: Boolean = {
    configuration.getBoolean("mmp.daily.check.enabled")
      .getOrElse(true)
  }

  /** Как часто надо проверять таблицу advsOK на предмет необходимости изменений в выдаче. */
  private def CHECK_ADVS_OK_DURATION: FiniteDuration = {
    configuration.getInt("mmp.daily.check.advs.ok.every.seconds")
      .getOrElse(60)
      .seconds
  }

  /** Не раньше какого времени можно запускать auto-accept. */
  //private val AUTO_ACCEPT_REQS_AFTER_HOURS = configuration.getInt("mmp.daily.accept.auto.after.hours") getOrElse 16

  /** Сборщик спеки периодических задач биллинга. */
  override def cronTasks(app: Application): TraversableOnce[ICronTask] = {
    val enabled = CRON_BILLING_CHECK_ENABLED
    info("enabled = " + enabled)
    if (enabled) {
      val every = CHECK_ADVS_OK_DURATION

      /*val applyOldReqs = MCronTask(
        startDelay = 3.seconds,
        every = every,
        displayName = "enableOfflineAdvs()"
      ) {
        enableOfflineAdvs()
      }*/

      val depubExpired = MCronTask(
        startDelay = 5.seconds,
        every = every,
        displayName = "depublishExpiredAdvs()"
      ) {
        deaFac.create().run()
      }

      val advOfflineAdvs = MCronTask(
        startDelay = 15.seconds,
        every = every,
        displayName = "advertiseOfflineAds()"
      ) {
        aoaFac.create().run()
      }
      List(depubExpired, advOfflineAdvs)

    } else {
      Nil
    }
  }

  /** Цикл автоматического накатывания MAdvReq в MAdvOk.
    * Нужно найти висячие MAdvReq и заапрувить их не проверяя. */
  /*def enableOfflineAdvs() {
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
        advDirectBilling.acceptAdvReq(mar, isAuto = true)
      }
    }
  }*/

}
