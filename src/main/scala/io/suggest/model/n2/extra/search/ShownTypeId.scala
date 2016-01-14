package io.suggest.model.n2.extra.search

import io.suggest.model.n2.node.MNodeFields
import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 15:28
 * Description: Поддержка dyn-поиска по полю shownTypeIds.
 */
trait ShownTypeId extends DynSearchArgs {

  /** Искать/фильтровать по shownTypeId узла. */
  def shownTypeIds: Seq[String]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _sti = shownTypeIds
    if (_sti.isEmpty) {
      qbOpt0

    } else {
      val fn = MNodeFields.Extras.ADN_SHOWN_TYPE_FN
      qbOpt0 map { qb =>
        val stiFilter = FilterBuilders.termsFilter(fn, _sti: _*)
        QueryBuilders.filteredQuery(qb, stiFilter)
      } orElse {
        val stiQuery = QueryBuilders.termsQuery(fn, _sti: _*)
          .minimumMatch(1) // может быть только один тип ведь у одного узла.
        Some(stiQuery)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(shownTypeIds, super.sbInitSize)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("shownTypeIds", shownTypeIds, super.toStringBuilder)
  }
}


trait ShownTypeIdDflt extends ShownTypeId {
  override def shownTypeIds: Seq[String] = Nil
}


trait ShownTypeIdWrap extends ShownTypeId with DynSearchArgsWrapper {
  override type WT <: ShownTypeId
  override def shownTypeIds = _dsArgsUnderlying.shownTypeIds
}
