package util.geo

import javax.inject.Inject
import io.suggest.loc.geo.ipgeobase.IpgbImporter
import models.mcron.MCronTask
import models.mproj.ICommonDi
import util.cron.ICronTasksProvider
import io.suggest.common.empty.OptionUtil.BoolOptOps

import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.14 14:03
 * Description: Утиль для поддержки БД, взятых из [[http://ipgeobase.ru/]].
 */
class IpGeoBaseImport @Inject() (
                                  mCommonDi    : ICommonDi
                                )
  extends ICronTasksProvider
{

  import mCommonDi._

  /** Активация импорта требует явного включения этой функции в конфиге.
    * Отключена по умолчанию, т.к. она должна быть активна только на одной ноде. */
  override def isEnabled: Boolean = configuration
    .getOptional[Boolean]("ipgeobase.import.enabled")
    .getOrElseFalse

  private def _ipgbReImportTask = MCronTask(
    // TODO Нужно обновлять 1-2 раза в день максимум, а не после каждого запуска.
    startDelay  = 20.seconds,
    every       = 1.day,
    displayName = "updateIpBase()"
  ) { () =>
    current.injector
      .instanceOf[IpgbImporter]
      .updateIpBase()
  }

  override def cronTasks(): Iterable[MCronTask] = {
    _ipgbReImportTask #::
    LazyList.empty
  }

}

