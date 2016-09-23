package io.suggest.model.es

import io.suggest.util.MacroLogsI
import org.elasticsearch.common.settings.Settings
import io.suggest.util.SioEsUtil.laFuture2sFuture

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 12:03
  * Description: Трейт поддержки создания индекса с каким-то произвольным именем.
  * Используется в index-моделях со "скользящими" во времени индексами: ipgeobase, статистика, логи.
  */

trait EsIndexDynCreate extends IEsModelDi with MacroLogsI {

  import mCommonDi._

  /** Собрать новый индекс для заливки туда моделей ipgeobase. */
  def createIndex(newIndexName: String): Future[_] = {
    val fut: Future[_] = {
      esClient.admin().indices()
        .prepareCreate(newIndexName)
        // Надо сразу отключить index refresh в целях оптимизации bulk-заливки в индекс.
        .setSettings( indexSettingsCreate )
        .execute()
    }

    lazy val logPrefix = s"createIndex($newIndexName):"
    fut.onComplete {
      case Success(res) => LOGGER.debug(s"$logPrefix Ok, $res")
      case Failure(ex)  => LOGGER.error(s"$logPrefix failed", ex)
    }

    fut
  }

  /** Сгенерить настройки для создаваемого индекса. */
  protected def indexSettingsCreate: Settings

}



