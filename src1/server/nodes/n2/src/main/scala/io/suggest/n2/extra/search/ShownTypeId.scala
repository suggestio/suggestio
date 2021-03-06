package io.suggest.n2.extra.search

import io.suggest.es.search.DynSearchArgs
import io.suggest.n2.node.MNodeFields
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 15:28
 * Description: Поддержка dyn-поиска по полю shownTypeIds.
 */
trait ShownTypeId extends DynSearchArgs {

  /** Искать/фильтровать по shownTypeId узла. */
  def shownTypeIds: Seq[String] = Nil

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _sti = shownTypeIds
    if (_sti.isEmpty) {
      qbOpt0

    } else {
      val fn = MNodeFields.Extras.ADN_SHOWN_TYPE_FN
      val stiQ = QueryBuilders.termsQuery(fn, _sti: _*)
      qbOpt0.map { qb =>
        QueryBuilders.boolQuery()
          .must(qb)
          .filter(stiQ)
      }.orElse {
        Some(stiQ)
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
