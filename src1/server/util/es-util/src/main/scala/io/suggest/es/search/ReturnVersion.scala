package io.suggest.es.search

import org.elasticsearch.action.search.SearchRequestBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 17:46
 * Description: DynSearch-аддоны для поля запроса version (bool), которое управляет необходимостью
 * для ES определять или не определять точное значение _version для возвращаемых результатов поиска.
 */
trait ReturnVersion extends DynSearchArgs {

  /** Возвращать ли _version в результатах? */
  def returnVersion: Option[Boolean] = None

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)

    for (isReturnVersion <- returnVersion) {
      srb
        .setVersion( isReturnVersion )
        .seqNoAndPrimaryTerm( isReturnVersion )
    }

    srb1
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("retVsn", returnVersion, super.toStringBuilder)
  }

  override def sbInitSize: Int = {
    val l0 = super.sbInitSize
    val rv = returnVersion
    if (rv.isDefined) {
      l0 + 18
    } else {
      l0
    }
  }

}
