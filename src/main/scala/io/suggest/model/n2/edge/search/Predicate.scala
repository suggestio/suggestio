package io.suggest.model.n2.edge.search

import io.suggest.model.n2.edge.MPredicate
import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders, QueryBuilder}
import io.suggest.model.n2.edge.MEdge.PREDICATE_ID_FN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 13:41
 * Description: Поисковый аддон для выборки по полю [[io.suggest.model.n2.edge.MEdge]].predicate.
 */
trait Predicate extends DynSearchArgs {

  /** Предикат или предикаты, по которым требуется OR-выборка. */
  def predicates: Seq[MPredicate]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _preds = predicates
    if (_preds.isEmpty) {
      qbOpt0

    } else {
      qbOpt0.map { qb0 =>
        val pf = FilterBuilders.termsFilter(PREDICATE_ID_FN, _preds: _*)
          .execution("or")
        QueryBuilders.filteredQuery(qb0, pf)
      }
      .orElse {
        val qb = QueryBuilders.termsQuery(PREDICATE_ID_FN, _preds: _*)
        Some(qb)
      }
    }
  }

  override def sbInitSize: Int = {
    val sz0 = super.sbInitSize
    val _preds = predicates
    if (_preds.isEmpty) {
      sz0
    } else {
      _preds.foldLeft(sz0 + 16) { (acc, pred) =>
        pred.strId.length + acc + 2
      }
    }
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("predicates", predicates, super.toStringBuilder)
  }

}


/** Дефолтовая реализация полей трейта [[Predicate]]. */
trait PredicateDflt extends Predicate {
  override def predicates: Seq[MPredicate] = Nil
}


/** Wrap-реализация трейта [[Predicate]]. */
trait PredicateWrap extends Predicate with DynSearchArgsWrapper {
  override type WT <: Predicate
  override def predicates = _dsArgsUnderlying.predicates
}
