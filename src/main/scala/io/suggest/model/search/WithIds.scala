package io.suggest.model.search

import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.12.14 10:42
 * Description: Поиск/фильтрация по списку значений _id.
 */
trait WithIds extends DynSearchArgs {

  /** Искать только результаты, имеющие указанные _id. */
  def withIds: Seq[String]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _withIds = withIds
    if (_withIds.isEmpty) {
      qbOpt0

    } else {
      qbOpt0.map { qb =>
        val idf = QueryBuilders.idsQuery(_withIds: _*)
        QueryBuilders.boolQuery()
          .must( qb )
          .filter( idf )
      }.orElse {
        val qb = QueryBuilders.idsQuery()
          .ids(_withIds: _*)
        Some(qb)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(withIds, super.sbInitSize)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("withIds", withIds, super.toStringBuilder)
  }

}


/** Дефолтовая реализация [[WithIds]]. */
trait WithIdsDflt extends WithIds {
  override def withIds: Seq[String] = Nil
}


/** Wrap-реализация [[WithIds]]. */
trait WithIdsWrap extends WithIds with DynSearchArgsWrapper {
  override type WT <: WithIds
  override def withIds = _dsArgsUnderlying.withIds
}
