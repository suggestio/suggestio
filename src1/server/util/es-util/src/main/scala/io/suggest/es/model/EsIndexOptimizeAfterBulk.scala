package io.suggest.es.model

import io.suggest.es.util.SioEsUtil.EsActionBuilderOpsExt
import io.suggest.util.logs.IMacroLogs
import org.elasticsearch.common.settings.Settings

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 12:12
  * Description: API для оптимизации индекса после bulk load'а данных.
  */
trait EsIndexOptimizeAfterBulk extends IEsModelDi with IMacroLogs {

  import mCommonDi._


  /** Когда заливка данных закончена, выполнить подготовку индекса к эсплуатации.
    * elasticsearch 2.0+: переименовали операцию optimize в force merge. */
  final def optimizeAfterBulk(newIndexName: String): Future[_] = {
    val startedAt = System.currentTimeMillis()

    // Запустить оптимизацию всего ES-индекса.
    val inxOptFut: Future[_] = {
      esClient.admin().indices()
        .prepareForceMerge(newIndexName)
        .setMaxNumSegments(1)
        .setFlush(true)
        .executeFut()
    }

    lazy val logPrefix = s"optimizeAfterBulk($newIndexName):"

    // Потом нужно выставить не-bulk настройки для готового к работе индекса.
    val inxSettingsFut = inxOptFut.flatMap { _ =>
      val updInxSettingsAt = System.currentTimeMillis()

      val fut2: Future[_] = indexSettingsAfterBulk.fold[Future[_]] {
        Future.successful(None)
      } { settings2 =>
        esClient.admin().indices()
          .prepareUpdateSettings(newIndexName)
          .setSettings( settings2 )
          .executeFut()
      }

      LOGGER.trace(s"$logPrefix Optimize took ${updInxSettingsAt - startedAt} ms")
      for (_ <- fut2)
        LOGGER.trace(s"$logPrefix Update index settings took ${System.currentTimeMillis() - updInxSettingsAt} ms.")

      fut2
    }

    inxSettingsFut
  }


  /** Новые настройки существующего. */
  def indexSettingsAfterBulk: Option[Settings]

}
