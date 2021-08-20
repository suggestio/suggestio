package util.billing.cron

import javax.inject.Inject
import io.suggest.util.logs.MacroLogsDyn
import models.mcron.MCronTask
import util.billing.Bill2Util
import util.cron.ICronTasksProvider
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.mbill2.m.ott.MOneTimeTokens
import io.suggest.model.SlickHolder
import japgolly.univeq._
import play.api.Configuration
import play.api.inject.Injector

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.11.15 11:16
 * Description: Провайдер задач Биллинга v2+ для Cron'а.
 * Т.к. все товары и услуги были унифицированы через MItem, то их обслуживание так-же потребовало унификации.
 */
class BillingCronTasks @Inject()(
                                  injector: Injector,
                                )
  extends ICronTasksProvider
  with MacroLogsDyn
{

  private def slickHolder = injector.instanceOf[SlickHolder]
  private def configuration = injector.instanceOf[Configuration]
  implicit private def ec = injector.instanceOf[ExecutionContext]


  /** Включен ли биллинг по крону? Будет выполнятся публикация карточек, их сокрытие и т.д. */
  override def isEnabled: Boolean = configuration
    .getOptional[Boolean]("bill.cron.enabled")
    .getOrElseTrue

  /** Как часто надо проверять таблицу advsOK на предмет необходимости изменений в выдаче. */
  private def ADVS_EVERY = 20.seconds


  private def depubExpired = MCronTask(
    startDelay  = 12.seconds,
    every       = ADVS_EVERY,
    displayName = "depublishExpiredAdvs()",
  ) { () =>
    injector
      .instanceOf[DisableExpiredAdvsFactory]
      .create()
      .run()
  }

  private def advOfflineAdvs = MCronTask(
    startDelay  = 17.seconds,
    every       = ADVS_EVERY,
    displayName = "advertiseOfflineAds()",
  ) { () =>
    injector
      .instanceOf[ActivateOfflineAdvsFactory]
      .create()
      .run()
  }

  private def unStallHoldedOrders = MCronTask(
    startDelay  = 30.seconds,
    every       = 10.minutes,
    displayName = "unStallHoldedOrders()",
  ) { () =>
    for {
      ex <- injector
        .instanceOf[Bill2Util]
        .findReleaseStalledHoldOrders()
        .failed
    } {
      LOGGER.error("Failed to findReleaseStalledHoldOrders()", ex)
    }
  }

  private def ottDeleteOld = MCronTask(
    // Удаление старых одноразовых токенов, которые живут в биллинге.
    startDelay  = 1.minute,
    every       = 5.minutes,
    displayName = "deleteOldOtt",
  ) { () =>
    for {
      countDeleted <- slickHolder.slick.db.run {
        injector
          .instanceOf[MOneTimeTokens]
          .deleteOld()
      }
    } {
      if (countDeleted > 0)
        LOGGER.trace(s"Deleted $countDeleted otts.")
    }
  }


  /** Сборщик спеки периодических задач биллинга. */
  override def cronTasks(): Iterable[MCronTask] = {
    depubExpired #::
    advOfflineAdvs #::
    unStallHoldedOrders #::
    ottDeleteOld #::
    LazyList.empty
  }

}
