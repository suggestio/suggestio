package util.billing.cron

import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import models.mcron.MCronTask
import models.mproj.ICommonDi
import util.billing.Bill2Util
import util.cron.ICronTasksProvider
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.mbill2.m.ott.MOneTimeTokens

import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.11.15 11:16
 * Description: Провайдер задач Биллинга v2+ для Cron'а.
 * Т.к. все товары и услуги были унифицированы через MItem, то их обслуживание так-же потребовало унификации.
 */
class BillingCronTasks @Inject()(
                                  aoaFac                  : ActivateOfflineAdvsFactory,
                                  deaFac                  : DisableExpiredAdvsFactory,
                                  bill2Util               : Bill2Util,
                                  mOneTimeTokens          : MOneTimeTokens,
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
      .getOrElseTrue
  }

  /** Как часто надо проверять таблицу advsOK на предмет необходимости изменений в выдаче. */
  private def CHECK_ADVS_OK_DURATION: FiniteDuration = {
    configuration.getOptional[Int]("bill.cron.check.every.seconds")
      .getOrElse(20)
      .seconds
  }


  /** Сборщик спеки периодических задач биллинга. */
  override def cronTasks(): TraversableOnce[MCronTask] = {
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
        for {
          ex <- bill2Util.findReleaseStalledHoldOrders().failed
        } {
          LOGGER.error("Failed to findReleaseStalledHoldOrders()", ex)
        }
      }

      // Удаление старых одноразовых токенов, которые живут в биллинге.
      val ottDeleteOld = MCronTask(
        startDelay = 1 minute,
        every = 5 minutes,
        displayName = "deleteOldOtt"
      ) {
        for {
          countDeleted <- slick.db.run {
            mOneTimeTokens.deleteOld()
          }
        } yield {
          if (countDeleted > 0)
            LOGGER.debug(s"Deleted $countDeleted otts.")
        }
      }

      depubExpired ::
        advOfflineAdvs ::
        unStallHoldedOrders ::
        ottDeleteOld ::
        Nil

    } else {
      Nil
    }
  }

}
