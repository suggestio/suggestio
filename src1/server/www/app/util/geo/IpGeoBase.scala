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

  /** Вернуть инстанс класса для импорта IP Geo Base.
    * Т.к. этот инстанс нужен раз в день, то нет никакого смысла его хранить в голове. */
  private def ipgbImporter = current.injector.instanceOf[IpgbImporter]

  /** Активация импорта требует явного включения этой функции в конфиге.
    * Отключена по умолчанию, т.к. она должна быть активна только на одной ноде. */
  def IS_ENABLED: Boolean = configuration.getOptional[Boolean]("ipgeobase.import.enabled")
    .getOrElseFalse

  override def cronTasks(): Iterable[MCronTask] = {
    if (IS_ENABLED) {
      // TODO Нужно обновлять 1-2 раза в день максимум, а не после каждого запуска.
      val task = MCronTask(
        startDelay  = 20.seconds,
        every       = 1.day,
        displayName = "updateIpBase()"
      ) {
        ipgbImporter.updateIpBase()
      }
      task :: Nil

    } else {
      Nil
    }
  }

}

