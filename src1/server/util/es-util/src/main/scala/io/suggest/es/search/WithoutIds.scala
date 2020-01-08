package io.suggest.es.search

import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.12.14 22:34
 * Description: Отрицательная фильтрация es-документов по их _id.
 */
trait WithoutIds extends DynSearchArgs with IEsTypes {

  /** Отбрасывать документы, имеющие указанные id'шники. */
  def withoutIds: Seq[String] = Nil

  /** Собрать экземпляр ES QueryBuilder на основе имеющихся в экземпляре данных.
    * Здесь можно навешивать дополнительные фильтры, выполнять pre- и post-процессинг запроса. */
  override def toEsQuery: QueryBuilder = {
    val query3 = super.toEsQuery
    // Если включен withoutIds, то нужно обернуть query3 в соответствующий not(ids filter).
    if (withoutIds.nonEmpty) {
      QueryBuilders.boolQuery()
        .must(query3)
        .mustNot {
          QueryBuilders.idsQuery(esTypes: _*)
            .addIds(withoutIds: _*)
        }
    } else {
      query3
    }
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
