package util.billing.cron

import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import models.mcron.{ICronTask, MCronTask}
import models.mproj.ICommonDi
import util.adv.direct.AdvDirectBilling
import util.billing.Bill2Util
import util.cron.ICronTasksProvider

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
  bill2Util               : Bill2Util,
  mCommonDi               : ICommonDi
)
  extends ICronTasksProvider
  with MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Включен ли биллинг по крону? Будет выполнятся публикация карточек, их сокрытие и т.д. */
  private def CRON_BILLING_CHECK_ENABLED: Boolean = {
    configuration.getOptional[Boolean]("bill.cron.enabled")
      .getOrElse(true)
  }

  /** Как часто надо проверять таблицу advsOK на предмет необходимости изменений в выдаче. */
  private def CHECK_ADVS_OK_DURATION: FiniteDuration = {
    configuration.getOptional[Int]("bill.cron.check.every.seconds")
      .getOrElse(20)
      .seconds
  }

  /** Не раньше какого времени можно запускать auto-accept. */
  //private val AUTO_ACCEPT_REQS_AFTER_HOURS = configuration.getInt("mmp.daily.accept.auto.after.hours") getOrElse 16

  /** Сборщик спеки периодических задач биллинга. */
  override def cronTasks(): TraversableOnce[ICronTask] = {
    val enabled = CRON_BILLING_CHECK_ENABLED
    info("enabled = " + enabled)
    if (enabled) {
      val every = CHECK_ADVS_OK_DURATION

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

      val unStallHoldedOrders = MCronTask(
        startDelay = 30.seconds,
        every = 10.minutes,
        displayName = "unStallHoldedOrders()"
      ) {
        bill2Util.findReleaseStalledHoldOrders()
          .onFailure { case ex: Throwable =>
            LOGGER.error("Failed to findReleaseStalledHoldOrders()", ex)
          }
      }
      List(depubExpired, advOfflineAdvs, unStallHoldedOrders)

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
