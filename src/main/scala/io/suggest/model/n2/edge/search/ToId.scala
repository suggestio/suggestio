package io.suggest.model.n2.edge.search

import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.QueryBuilder
import io.suggest.model.n2.edge.MEdge.TO_ID_FN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 13:53
 * Description: Поисковый аддон для выборки по полю [[io.suggest.model.n2.edge.MEdge]].toId.
 */
trait ToId extends DynSearchArgs {

  def toId: Seq[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    Util.strSeqToQueryOpt(TO_ID_FN, toId, super.toEsQueryOpt)
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    Util.sbInitSz(super.sbInitSize, 4, toId)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("toId", toId, super.toStringBuilder)
  }

}


/** Дефолтовая реализация полей аддона [[ToId]]. */
trait ToIdDflt extends ToId {
  override def toId: Seq[String] = Nil
}


/** Wrap-реализация полей аддона [[ToId]]. */
trait ToIdWrap extends ToId with DynSearchArgsWrapper {
  override type WT <: ToId
  override def toId = _dsArgsUnderlying.toId
}
