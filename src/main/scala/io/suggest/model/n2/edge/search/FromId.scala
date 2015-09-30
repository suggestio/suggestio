package io.suggest.model.n2.edge.search

import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.QueryBuilder
import io.suggest.model.n2.edge.MEdge.FROM_ID_FN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 12:49
 * Description: Аддоны для поддержки поиска в поле fromId.
 */
trait FromId extends DynSearchArgs {

  /** id n2-узла, ссылающегося искомыми ребрами графа на другие узлы. */
  def fromId: Seq[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    Util.strSeqToQueryOpt(FROM_ID_FN, fromId, super.toEsQueryOpt)
  }

  override def sbInitSize: Int = {
    Util.sbInitSz(super.sbInitSize, 6, fromId)
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("fromId", fromId, super.toStringBuilder)
  }

}


/** Дефолтовая реализация полей поискового аддона [[FromId]]. */
trait FromIdDflt extends FromId {
  override def fromId: Seq[String] = Nil
}


/** Враппер-реализация полей поискового аддона [[FromId]]. */
trait FromIdWrap extends FromId with DynSearchArgsWrapper {
  override type WT <: FromId
  override def fromId = _dsArgsUnderlying.fromId
}
