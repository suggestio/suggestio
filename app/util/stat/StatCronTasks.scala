package util.stat

import com.google.inject.Inject
import io.suggest.stat.inx.StatIndexUtil
import models.mcron.{ICronTask, MCronTask}
import models.mproj.ICommonDi
import util.PlayMacroLogsDyn
import util.cron.ICronTasksProvider

import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 20:24
  * Description: Поддержка cron-задач для нужд статистики.
  */
class StatCronTasks @Inject()(
  mCommonDi : ICommonDi
)
  extends ICronTasksProvider
  with PlayMacroLogsDyn
{

  import mCommonDi._

  private def statIndexUtil = current.injector.instanceOf[StatIndexUtil]

  private def _CONF_KEY = "stat.cron.enabled"

  /**
    * Флаг активности этих cron-задач.
    * По умолчанию - выключено, т.е. надо вручную активировать в конфиге.
    */
  def IS_ENABLED = configuration.getBoolean(_CONF_KEY).contains(true)

  /** Список задач, которые надо вызывать по таймеру. */
  override def cronTasks(): TraversableOnce[ICronTask] = {
    if (IS_ENABLED) {
      List(
        // Создание новых stat-индексов и переключение на них
        MCronTask(
          startDelay  = 1.second,
          every       = 24.hours,
          displayName = "stat inx renew"
        ) {
          statIndexUtil.maybeReNewCurrIndex()
        },

        // Удаление слишком старых индексов.
        MCronTask(
          startDelay = 1.minute,
          every = 48.hour,
          displayName = "stat inx old delete"
        ) {
          statIndexUtil.maybeDeleteTooOldIndex()
        }
      )

    } else {
      LOGGER.info(s"cron module disabled: ${_CONF_KEY}")
      Nil
    }
  }

}
