package io.suggest.es.model

import io.suggest.util.MacroLogsI
import io.suggest.es.util.SioEsUtil.laFuture2sFuture
import org.elasticsearch.ResourceNotFoundException

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 11:56
  * Description: Трейт с утилями для добавления поддержки управления статическим index alias
  */
trait EsIndexStaticAlias extends IEsModelDi with MacroLogsI {

  import mCommonDi._

  protected def INDEX_ALIAS_NAME: String


  /** Выставить алиас на текущий индекс, забыв о предыдущих данных алиаса. */
  def resetIndexAliasTo(newIndexName: String): Future[_] = {
    val aliasName = INDEX_ALIAS_NAME
    val fut = esClient.admin().indices()
      .prepareAliases()
      // Удалить все алиасы с необходимым именем.
      .removeAlias("*", aliasName)
      // Добавить алиас на новый индекс.
      .addAlias(newIndexName, aliasName)
      .execute()

    // Подключить логгирование к работе...
    lazy val logPrefix = s"installIndexAliase($newIndexName <= $aliasName):"
    fut.onComplete {
      case Success(r)  => LOGGER.debug(s"$logPrefix OK, result = $r")
      case Failure(ex) => LOGGER.error(s"$logPrefix Failed to update index alias $aliasName", ex)
    }

    fut
  }


  /** Эксперимент с хранением статического log-префикса вне метода. */
  private def _gAIN_logPrefix = "getAliasesIndexName():"

  /** Узнать имя индекса, сокрытого за глобальным алиасом INDEX_ALIAS_NAME. */
  def getAliasedIndexName(): Future[Set[String]] = {
    val fut = esClient.admin().indices()
      .prepareGetAliases(INDEX_ALIAS_NAME)
      .execute()

    fut
      .map { resp =>
        LOGGER.trace(s"${_gAIN_logPrefix} Ok, found ${resp.getAliases.size()} indexes.")
        resp.getAliases
          .keysIt()
          .toSet
      }
      .recover { case ex: ResourceNotFoundException =>
        // Если алиасов не найдено, ES обычно возвращает ошибку 404. Это тоже отработать надо бы.
        LOGGER.warn(s"${_gAIN_logPrefix} 404, suppressing error to empty result.", ex)
        Set.empty
      }
  }

}
