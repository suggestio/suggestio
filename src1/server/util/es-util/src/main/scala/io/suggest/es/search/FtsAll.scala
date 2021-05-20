package io.suggest.es.search

import org.elasticsearch.index.query.QueryBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 10:30
 * Description: Example search addon for full-text-searching.
 */

trait FtsAll extends DynSearchArgs {

  def ftsSearchFieldName: String

  /** Full-text query, if any. */
  // TODO Must include filed names inside Option[FtsQuery], not just string. Remove ftsSearchField above.
  def ftsQuery: Option[String] = None

  override def toEsQueryOpt: Option[QueryBuilder] = {
    TextQuerySearch.mkEsQuery(ftsSearchFieldName, ftsQuery, super.toEsQueryOpt)
  }

  override def sbInitSize: Int = {
    collStringSize(ftsQuery, super.sbInitSize, addOffset = ftsSearchFieldName.length + 1)
  }

  override def toStringBuilder: StringBuilder = {
    val qOptWithFn = ftsQuery.map(ftsSearchFieldName + ":" + _)
    fmtColl2sb("ftsQuery", qOptWithFn, super.toStringBuilder)
  }

}
