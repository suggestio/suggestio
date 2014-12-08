package io.suggest.ym.model.ad

import io.suggest.ym.model.common.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.12.14 10:42
 * Description: Поиск/фильтрация по списку значений _id.
 */
trait WithIdsDsa extends DynSearchArgs {

  /** Искать только результаты, имеющие указанные _id. */
  def withIds: Seq[String]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      if (withIds.nonEmpty) {
        val idf = FilterBuilders.idsFilter().ids(withIds: _*)
        QueryBuilders.filteredQuery(qb, idf)
      } else {
        qb
      }
    }.orElse[QueryBuilder] {
      if (withIds.nonEmpty) {
        val qb = QueryBuilders.idsQuery().ids(withIds: _*)
        Some(qb)
      } else {
        None
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


trait WithIdsDsaDflt extends WithIdsDsa {
  override def withIds: Seq[String] = Seq.empty
}


trait WithIdsDsaWrapper extends WithIdsDsa with DynSearchArgsWrapper {
  override type WT <: WithIdsDsa
  override def withIds = _dsArgsUnderlying.withIds
}
