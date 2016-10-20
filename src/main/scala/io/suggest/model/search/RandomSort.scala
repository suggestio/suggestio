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
  def randomSort: Option[MRandomSortData]

  /** Собрать экземпляр ES QueryBuilder на основе имеющихся в экземпляре данных.
    * Здесь можно навешивать дополнительные фильтры, выполнять pre- и post-процессинг запроса. */
  override def toEsQuery: QueryBuilder = {
    val q0 = super.toEsQuery
    randomSort.fold(q0) { rs =>
      // Можно рандомно сортировать с учётом generation.
      val scoreFun = ScoreFunctionBuilders.randomFunction( rs.generation )

      // Нормировать рандомное значение. Оно гуляет до Int.MaxValue (2.14e9), а такой разбег может навредить
      // при попытках поднять какие-либо результаты поика над остальными. Например, поиск в маячках на фоне обычного поиска.
      for (weight <- rs.weight) {
        scoreFun.setWeight( weight )
      }

      QueryBuilders.functionScoreQuery(q0, scoreFun)
    }
  }

  override def sbInitSize: Int = {
    val sz0 = super.sbInitSize
    val rs = randomSort
    if (rs.isEmpty) sz0 else sz0 + 20
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("randomSort", randomSort, super.toStringBuilder)
  }

}

/** Дефолтовая реализация [[RandomSort]]. */
trait RandomSortDflt extends RandomSort {
  override def randomSort: Option[MRandomSortData] = None
}

/** Враппер-реализация [[RandomSort]]. */
trait RandomSortWrap extends RandomSort with DynSearchArgsWrapper {
  override type WT <: RandomSort

  override def randomSort = _dsArgsUnderlying.randomSort
}


case class MRandomSortData(
  generation  : Long,
  weight      : Option[Float] = None
)
