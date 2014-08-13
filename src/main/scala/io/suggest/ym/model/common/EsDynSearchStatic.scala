package io.suggest.ym.model.common

import io.suggest.model.EsModelMinimalStaticT
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

trait EsDynSearchStatic[A <: DynSearchArgs] extends EsModelMinimalStaticT {

  /** Лимитировать кол-во результатов на выходе поиска.
    * Если args.maxResults выше нормы, то будет использован этот лимит. */
  def DYN_SEARCH_MAX_RESULTS_HARD = 200

  /** Билдер поискового запроса. */
  def dynSearchReqBuilder(dsa: A)(implicit ec:ExecutionContext, client: Client) = {
    // Запускаем собранный запрос.
    prepareSearch
      .setQuery(dsa.toEsQuery)
      .setSize(Math.min(DYN_SEARCH_MAX_RESULTS_HARD, Math.max(1, dsa.maxResults)))
      .setFrom(Math.max(0, dsa.offset))
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
      .setNoFields()
      .execute()
      .flatMap { searchResp2RtMultiget(_) }
  }

}


/** Базовый интерфейс для аргументов-критериев поиска. */
trait DynSearchArgs {

  /** Макс.кол-во результатов. */
  def maxResults: Int

  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  def offset: Int

  /** Собрать экземпляр ES QueryBuilder на основе имеющихся в экземпляре данных. */
  def toEsQuery: QueryBuilder

}

