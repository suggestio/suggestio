package io.suggest.ym.model.ad

import io.suggest.ym.model.common.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.12.14 22:34
 * Description: Поисковые аддоны для отрицательной фильтрации es-документов по id.
 */
trait WithoutIdsDsa extends DynSearchArgs {

  /** Отбрасывать документы, имеющие указанные id'шники. */
  def withoutIds: Seq[String]

  /** Собрать экземпляр ES QueryBuilder на основе имеющихся в экземпляре данных.
    * Здесь можно навешивать дополнительные фильтры, выполнять pre- и post-процессинг запроса. */
  override def toEsQuery: QueryBuilder = {
    var query3: QueryBuilder = super.toEsQuery
    // Если включен withoutIds, то нужно обернуть query3 в соответствующий not(ids filter).
    if (withoutIds.nonEmpty) {
      val idsFilter = FilterBuilders.notFilter(
        FilterBuilders.idsFilter().addIds(withoutIds : _*)
      )
      query3 = QueryBuilders.filteredQuery(query3, idsFilter)
    }
    query3
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(withoutIds, super.sbInitSize)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("withoutIds", withoutIds, super.toStringBuilder)
  }
}

trait WithoutIdsDsaDflt extends WithoutIdsDsa {
  override def withoutIds: Seq[String] = Seq.empty
}

trait WithoutIdsDsaWrapper extends WithoutIdsDsa with DynSearchArgsWrapper {
  override type WT <: WithoutIdsDsa
  override def withoutIds = _dsArgsUnderlying.withoutIds
}
