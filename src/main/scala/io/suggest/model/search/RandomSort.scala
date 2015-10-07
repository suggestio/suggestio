package io.suggest.model.search

import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.12.14 22:25
 * Description: "Поколение" выдачи позволяет сортировать выдачу в случайном порядке,
 * исключая появления дубликатов между страницами.
 * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-function-score-query.html#function-random]]
 */
trait RandomSort extends DynSearchArgs {

  /**
   * Значение sorting seed.
   * @return None, то значит random-сортировка отключена.
   *         Some() с random seed, которое будет использовано для сортировки.
   */
  def randomSortSeed: Option[Long]

  /** Собрать экземпляр ES QueryBuilder на основе имеющихся в экземпляре данных.
    * Здесь можно навешивать дополнительные фильтры, выполнять pre- и post-процессинг запроса. */
  override def toEsQuery: QueryBuilder = {
    var query = super.toEsQuery
    if (randomSortSeed.isDefined) {
      // Можно и нужно сортировтать с учётом genTs. Точный скоринг не нужен, поэтому просто прикручиваем скипт для скоринга.
      val scoreFun = ScoreFunctionBuilders.randomFunction( randomSortSeed.get )
      query = QueryBuilders.functionScoreQuery(query, scoreFun)
    }
    query
  }

}


trait RandomSortDflt extends RandomSort {
  override def randomSortSeed: Option[Long] = None
}


trait RandomSortWrap extends RandomSort with DynSearchArgsWrapper {
  override type WT <: RandomSort

  override def randomSortSeed = _dsArgsUnderlying.randomSortSeed
}
