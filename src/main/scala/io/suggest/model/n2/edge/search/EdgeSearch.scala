package io.suggest.model.n2.edge.search

import io.suggest.model.search._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 14:34
 * Description: Утиль для поддержки поиска по [[io.suggest.model.n2.edge.MEdge]].
 */
trait EdgeSearch
  // Сначала идут хорошо-индексируемые критерии.
  extends FromId
  with ToId
  // И только потом filtered-критерии и прочий мусор.
  with Predicate
  with Ordering
  with Limit
  with Offset


/** Трейт дефолтовых значений полей поиска. */
trait EdgeSearchDflt
  extends EdgeSearch
  with FromIdDflt
  with ToIdDflt
  with PredicateDflt
  with OrderingDflt
  with LimitDflt
  with OffsetDflt

/** Дефолтовая реализация для облегчения жизни компилятору. */
class EdgeSearchDfltImpl
  extends EdgeSearchDflt


/** Wrap-реализция модели аргументов поиска [[io.suggest.model.n2.edge.MEdge]]. */
trait EdgeSearchWrap
  extends EdgeSearch
  with FromIdWrap
  with ToIdWrap
  with PredicateWrap
  with OrderingWrap
  with LimitWrap
  with OffsetWrap
{
  override type WT = EdgeSearchWrap
}
