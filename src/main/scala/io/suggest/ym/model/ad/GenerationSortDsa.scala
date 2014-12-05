package io.suggest.ym.model.ad

import io.suggest.ym.model.common.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.index.query.{QueryBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.12.14 22:25
 * Description: Поколение выдачи позволяет сортировать выдачу в случайном порядке, исключая появления дубликатов
 * между страницами.
 */
trait GenerationSortDsa extends DynSearchArgs {

  /** Значение Generation timestamp, генерится при первом обращении к выдаче и передаётся при последующих запросах выдачи. */
  def generationOpt: Option[Long]

  def generationSortingEnabled: Boolean

  /** Собрать экземпляр ES QueryBuilder на основе имеющихся в экземпляре данных.
    * Здесь можно навешивать дополнительные фильтры, выполнять pre- и post-процессинг запроса. */
  override def toEsQuery: QueryBuilder = {
    var query = super.toEsQuery
    if (generationOpt.isDefined && generationSortingEnabled) {
      // Можно и нужно сортировтать с учётом genTs. Точный скоринг не нужен, поэтому просто прикручиваем скипт для скоринга.
      val scoreFun = ScoreFunctionBuilders.randomFunction(generationOpt.get)
      query = QueryBuilders.functionScoreQuery(query, scoreFun)
    }
    query
  }

}

trait GenerationSortDsaDflt extends GenerationSortDsa {
  override def generationOpt: Option[Long] = None
  override def generationSortingEnabled: Boolean = true
}

trait GenerationSortDsaWrapper extends GenerationSortDsa with DynSearchArgsWrapper {
  override type WT <: GenerationSortDsa

  override def generationSortingEnabled = _dsArgsUnderlying.generationSortingEnabled
  override def generationOpt = _dsArgsUnderlying.generationOpt
}
