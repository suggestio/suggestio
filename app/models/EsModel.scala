package models

import scala.concurrent.Future
import util.SiowebEsUtil.client
import io.suggest.util.SioEsUtil.laFuture2sFuture
import play.api.libs.concurrent.Execution.Implicits._
import io.suggest.util.SioEsUtil
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 14:41
 * Description: Общее для elasticsearch-моделей лежит в этом файле. Обычно используется общий индекс для хранилища.
 */
object EsModel extends PlayMacroLogsImpl {

  import LOGGER._

  /** Имя индекса, который будет использоваться для хранения данных остальных моделей.
    * Имя должно быть коротким и лексикографически предшествовать именам остальных временных индексов. */
  val ES_INDEX_NAME = "-sio"

  def generateIndexSettings = SioEsUtil.getNewIndexSettings(shards=1, replicas=1)

  /**
   * Убедиться, что индекс существует.
   * @return Фьючерс для синхронизации работы. Если true, то новый индекс был создан.
   *         Если индекс уже существует, то false.
   */
  def ensureIndex: Future[Boolean] = {
    val adm = client.admin().indices()
    adm.prepareExists(ES_INDEX_NAME).execute().flatMap { existsResp =>
      if (existsResp.isExists) {
        Future.successful(false)
      } else {
        adm.prepareCreate(ES_INDEX_NAME)
          .setSettings(generateIndexSettings)
          .execute()
          .map { _ => true }
      }
    }
  }

  /**
   * Существует ли указанный маппинг в хранилище? Используется, когда модель хочет проверить наличие маппинга
   * внутри общего индекса.
   * @param typename Имя типа.
   * @return Да/нет.
   */
  def isMappingExists(typename: String): Future[Boolean] = {
    client.admin().cluster()
      .prepareState()
      .setFilterIndices(ES_INDEX_NAME)
      .execute()
      .map { cmd =>
        val imd = cmd.getState
          .getMetaData
          .index(ES_INDEX_NAME)
          .mapping(typename)
        trace("mapping exists resp: " + imd)
        imd != null
      }
  }

}
