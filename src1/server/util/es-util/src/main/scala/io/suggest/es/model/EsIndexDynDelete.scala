package io.suggest.es.model

import io.suggest.es.util.SioEsUtil.EsActionBuilderOpsExt
import io.suggest.util.logs.IMacroLogs
import org.elasticsearch.ResourceNotFoundException

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 12:09
  * Description: Трейт для добавления API удаления удаления динамического индекса.
  */
trait EsIndexDynDelete extends IEsModelDi with IMacroLogs {

  import mCommonDi._

  /** Логика удаления старого ненужного индекса. */
  def deleteIndex(oldIndexName: String): Future[_] = {
    val fut: Future[_] = esClient.admin().indices()
      .prepareDelete(oldIndexName)
      .executeFut()
      .map { _.isAcknowledged }

    val logPrefix = s"deleteIndex($oldIndexName):"

    // Отрабатывать ситуацию, когда индекс не найден.
    val fut1 = fut.recover { case ex: ResourceNotFoundException =>
      LOGGER.debug(s"$logPrefix Looks like, index not exist, already deleted?", ex)
      false
    }

    // Логгировать завершение команды.
    fut1.onComplete {
      case Success(res) => LOGGER.debug(s"$logPrefix Index deleted ok: $res")
      case Failure(ex)  => LOGGER.error(s"$logPrefix Failed to delete index", ex)
    }

    fut1
  }

}
