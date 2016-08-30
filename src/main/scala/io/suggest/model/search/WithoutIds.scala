package io.suggest.model.search

import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.12.14 22:34
 * Description: Поисковые аддоны для отрицательной фильтрации es-документов по id.
 */
trait WithoutIds extends DynSearchArgs {

  /** Отбрасывать документы, имеющие указанные id'шники. */
  def withoutIds: Seq[String]

  /** Собрать экземпляр ES QueryBuilder на основе имеющихся в экземпляре данных.
    * Здесь можно навешивать дополнительные фильтры, выполнять pre- и post-процессинг запроса. */
  override def toEsQuery: QueryBuilder = {
    var query3: QueryBuilder = super.toEsQuery
    // Если включен withoutIds, то нужно обернуть query3 в соответствующий not(ids filter).
    if (withoutIds.nonEmpty) {
      val idsFilter = QueryBuilders.notQuery(
        QueryBuilders.idsQuery(withoutIds: _*)
      )
      query3 = QueryBuilders.boolQuery()
        .must(query3)
        .filter(idsFilter)
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


trait WithoutIdsDflt extends WithoutIds {
  override def withoutIds: Seq[String] = Seq.empty
}


trait WithoutIdsWrap extends WithoutIds with DynSearchArgsWrapper {
  override type WT <: WithoutIds
  override def withoutIds = _dsArgsUnderlying.withoutIds
}
