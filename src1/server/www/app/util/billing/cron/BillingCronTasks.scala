package util.billing.cron

import javax.inject.Inject
import io.suggest.util.logs.MacroLogsDyn
import models.mcron.MCronTask
import models.mproj.ICommonDi
import util.billing.Bill2Util
import util.cron.ICronTasksProvider
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.mbill2.m.ott.MOneTimeTokens
import japgolly.univeq._

import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.11.15 11:16
 * Description: Провайдер задач Биллинга v2+ для Cron'а.
 * Т.к. все товары и услуги были унифицированы через MItem, то их обслуживание так-же потребовало унификации.
 */
class BillingCronTasks @Inject()(
                                  mCommonDi               : ICommonDi,
                                )
  extends ICronTasksProvider
  with MacroLogsDyn
{

  import mCommonDi.{configuration, slick, ec, current}


  /** Включен ли биллинг по крону? Будет выполнятся публикация карточек, их сокрытие и т.д. */
  override def isEnabled: Boolean = configuration
    .getOptional[Boolean]("bill.cron.enabled")
    .getOrElseTrue

  /** Как часто надо проверять таблицу advsOK на предмет необходимости изменений в выдаче. */
  private def ADVS_EVERY = 20.seconds


  private def depubExpired = MCronTask(
    startDelay  = 5.seconds,
    every       = ADVS_EVERY,
    displayName = "depublishExpiredAdvs()",
  ) { () =>
    current.injector
      .instanceOf[DisableExpiredAdvsFactory]
      .create()
      .run()
  }

  private def advOfflineAdvs = MCronTask(
    startDelay  = 15.seconds,
    every       = ADVS_EVERY,
    displayName = "advertiseOfflineAds()",
  ) { () =>
    current.injector
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
      ex <- current.injector
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
      countDeleted <- slick.db.run {
        current.injector
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
