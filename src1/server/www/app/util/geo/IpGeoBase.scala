package util.geo

import javax.inject.Inject
import io.suggest.geo.ipgeobase.IpgbImporter
import models.mcron.MCronTask
import util.cron.ICronTasksProvider
import io.suggest.common.empty.OptionUtil.BoolOptOps
import play.api.Configuration
import play.api.inject.Injector

import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.14 14:03
 * Description: Утиль для поддержки БД, взятых из [[http://ipgeobase.ru/]].
 */
class IpGeoBaseImport @Inject() (
                                  injector     : Injector,
                                )
  extends ICronTasksProvider
{

  private def configuration = injector.instanceOf[Configuration]
  private def ipgbImporter = injector.instanceOf[IpgbImporter]


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
  )( ipgbImporter.updateIpBase )

  override def cronTasks(): Iterable[MCronTask] = {
    _ipgbReImportTask #::
    LazyList.empty
  }

}

