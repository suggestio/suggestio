package util.stat

import javax.inject.Inject
import io.suggest.stat.inx.StatIndexUtil
import io.suggest.util.logs.MacroLogsDyn
import io.suggest.common.empty.OptionUtil.BoolOptOps
import models.mcron.MCronTask
import models.mproj.ICommonDi
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
  with MacroLogsDyn
{

  import mCommonDi._

  private def statIndexUtil = current.injector.instanceOf[StatIndexUtil]

  /**
    * Флаг активности этих cron-задач.
    * По умолчанию - выключено, т.е. надо вручную активировать в конфиге.
    */
  override def isEnabled = configuration
    .getOptional[Boolean]( "stat.cron.enabled" )
    .getOrElseFalse

  /** Создание новых stat-индексов и переключение на них */
  private def _reNewCurrentIndexTask = MCronTask(
    startDelay  = 10.second,
    every       = 12.hours,
    displayName = "stat inx renew"
  ) { () =>
    statIndexUtil.maybeReNewCurrIndex()
  }

  /** Удаление слишком старых индексов. */
  private def _deleteTooOldIndex = MCronTask(
    startDelay  = 1.minute,
    every       = 24.hour,
    displayName = "stat inx old delete"
  ) { () =>
    statIndexUtil.maybeDeleteTooOldIndex()
  }

  /** Список задач, которые надо вызывать по таймеру. */
  override def cronTasks(): Iterable[MCronTask] = {
    _reNewCurrentIndexTask #::
    _deleteTooOldIndex #::
    LazyList.empty
  }

}
