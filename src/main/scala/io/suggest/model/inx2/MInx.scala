package io.suggest.model.inx2

import org.elasticsearch.common.xcontent.XContentBuilder
import com.fasterxml.jackson.annotation.JsonIgnore
import org.elasticsearch.client.Client
import scala.concurrent.{Future, ExecutionContext}
import io.suggest.util.SioEsUtil.laFuture2sFuture
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.14 11:36
 * Description: В новой системе управления индексами (на смену MVI/MDVI-велосипедам) используется
 * ES в качестве хранилища.
 */
object MInx {

  val ES_INX_NAME_ESFN = "esInxName"

}


/** Платфрома для динамических частей inx2-моделей. Добавляет фунции управления индексами и всё необходимое. */
trait MInxT {

  def esInxNames: Seq[String]
  def esTypePrefix: String
  def esTypes: Seq[String]
  def esInxSettings(shards: Int, replicas: Int = 1): XContentBuilder

  /** Тут список обязательных маппингов, которые создаются в setMappings(). */
  @JsonIgnore def esInxMappings: Seq[(String, XContentBuilder)]

  /** Обновить index settings. */
  // TODO Функция по факту не работает. Надо разобраться с параметром number_of_shards.
  def setIndexSetting(implicit client: Client, ec: ExecutionContext): Future[Boolean] = {
    client.admin().indices()
      .prepareUpdateSettings(esInxNames : _*)
      .setSettings(esInxSettings(shards = 1).string())
      .execute()
      .map(_.isAcknowledged)
  }

  /** Выставить все маппинги, описанные в esInxMappings. */
  def setMappings(implicit client: Client, ec: ExecutionContext): Future[_] = {
    Future.traverse(esInxMappings) { case (_esTypeName, mappingXCB) =>
      client.admin().indices()
      .preparePutMapping(esInxNames : _*)
      .setType(_esTypeName)
      .setSource(mappingXCB)
      .execute()
    }
  }

  /** Удалить текущие маппинги из индексов. */
  def deleteMappings(implicit client: Client, ec: ExecutionContext): Future[_] = {
    Future.traverse(esInxMappings) { case (_esTypeName, _) =>
      client.admin().indices()
        .prepareDeleteMapping(esInxNames : _*)
        .setType(_esTypeName)
        .execute()
    }
  }

  /** Удалить индексы целиком. Операция стрёмная, должна использоваться только при полном реиндексе. */
  def eraseBackingIndices(implicit client: Client, ec: ExecutionContext): Future[_] = {
    client.admin().indices()
      .prepareDelete(esInxNames : _*)
      .execute()
  }


  /** Создать индексы. */
  def createIndex(shards: Int = 1, replicas: Int = 1)(implicit client: Client, ec: ExecutionContext): Future[Boolean] = {
    Future.traverse(esInxNames) { esInxName =>
      client.admin().indices()
        .prepareCreate(esInxName)
        .setSettings(esInxSettings(shards=shards, replicas=replicas))
        .execute()
        .map(_.isAcknowledged)
    } map { results =>
      results.reduce { _ && _ }
    }
  }


  /** Добавить необходимые фильтры в searchQuery чтобы правильно искать. */
  def addSearchQueryFilters(searchQuery: QueryBuilder): QueryBuilder = searchQuery

  /** Выставить параметры поискового реквеста.
    * Суффикс In в конце чтобы имя не конфликтовало с EsModelT.prepareSearch(). */
  def prepareSearchIn(implicit client: Client): SearchRequestBuilder = {
    client.prepareSearch(esInxNames : _*)
      .setTypes(esTypes : _*)
  }

  /** Выставить параметры поиского реквеста при удалении. */
  def prepareDeleteByQueryRequest(req: DeleteByQueryRequestBuilder): DeleteByQueryRequestBuilder = req

}


/** single-inx2 - это метаданные по индексам, всегда указывающие на один тип и один индекс.
  * Для таких индексов допустимы некоторые дополнительные фунции в static-моделях (getById() и т.д.). */
trait MSingleInxT extends MInxT {
  def targetEsInxName: String
  def targetEsType: String

  @JsonIgnore def esTypes = Seq(targetEsType)
  @JsonIgnore def esInxNames = Seq(targetEsInxName)
}

