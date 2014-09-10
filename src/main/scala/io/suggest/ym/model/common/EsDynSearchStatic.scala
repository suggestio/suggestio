package io.suggest.ym.model.common

import io.suggest.model.EsModelStaticT
import io.suggest.util.MacroLogsI
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilder
import io.suggest.util.SioEsUtil.laFuture2sFuture
import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.08.14 16:16
 * Description: Общий код для статических моделей и их запускалок динамически-генерируемых поисковых запросов.
 * Подразумевается, что Es Query генерятся на основе экземпляра, реализующего [[DynSearchArgs]].
 */

trait EsDynSearchStatic[A <: DynSearchArgs] extends EsModelStaticT with MacroLogsI {

  /** Билдер поискового запроса. */
  def dynSearchReqBuilder(dsa: A)(implicit client: Client): SearchRequestBuilder = {
    // Запускаем собранный запрос.
    val result = dsa.prepareSearchRequest(prepareSearch)
    LOGGER.trace(s"dynSearchReqBuilder($dsa): Compiled request = \n${result.toString}")
    result
  }

  /**
   * Поиск карточек в ТЦ по критериям.
   * @return Список рекламных карточек, подходящих под требования.
   */
  def dynSearch(dsa: A)(implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
    dynSearchReqBuilder(dsa)
      .execute()
      .map { searchResp2list }
  }

  /**
   * Аналог dynSearch, но возвращаются только id документов.
   * @param dsa Поисковый запрос.
   * @return Список id, подходящих под запрос, в неопределённом порядке.
   */
  def dynSearchIds(dsa: A)(implicit ec: ExecutionContext, client: Client): Future[Seq[String]] = {
    dynSearchReqBuilder(dsa)
      .setFetchSource(false)
      .setNoFields()
      .execute()
      .map { searchResp2idsList }
  }

  /**
   * Посчитать кол-во рекламных карточек, подходящих под запрос.
   * @param dsa Экземпляр, описывающий критерии поискового запроса.
   * @return Фьючерс с кол-вом совпадений.
   */
  def dynCount(dsa: A)(implicit ec: ExecutionContext, client: Client): Future[Long] = {
    // Необходимо выкинуть из запроса ненужные части.
    countByQuery(dsa.toEsQuery)
  }


  /** Поиск id, подходящих под запрос и последующий multiget. Используется для реалтаймого получения
    * изменчивых результатов, например поиск сразу после сохранения. */
  def dynSearchRt(dsa: A)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    dynSearchReqBuilder(dsa)
      .setFetchSource(false)
      .setNoFields()
      .execute()
      .flatMap { searchResp2RtMultiget(_) }
  }

}


/** Базовый интерфейс для аргументов-критериев поиска. */
trait DynSearchArgs {

  /** Жесткое ограничение сверху по кол-ву результатов поиска. По идее, оно не должно влиять на выдачу никогда.
    * Нужно для защиты от ddos при недостаточной проверке значения maxResults на верхнем уровне. */
  def MAX_RESULTS_HARD = 100

  /** Макс.кол-во результатов. */
  def maxResults: Int

  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  def offset: Int

  /** Собрать экземпляр ES QueryBuilder на основе имеющихся в экземпляре данных. */
  def toEsQuery: QueryBuilder

  /**
   * Сборка search-реквеста. Можно переопределить чтобы добавить в реквест какие-то дополнительные вещи,
   * кастомную сортировку например.
   * @param srb Поисковый реквест, пришедший из модели.
   * @return SearchRequestBuilder, наполненный данными по поисковому запросу.
   */
  def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    srb
      .setQuery(toEsQuery)
      .setSize(Math.min(MAX_RESULTS_HARD, Math.max(1, maxResults)))
      .setFrom(Math.max(0, offset))
  }

}

