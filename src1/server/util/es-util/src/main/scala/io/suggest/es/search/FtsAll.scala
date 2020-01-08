package io.suggest.es.search

import io.suggest.es.util.SioEsUtil
import org.elasticsearch.index.query.QueryBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 10:30
 * Description: Аддон для сборки базовых аргументов поиска карточек.
 */

trait FtsAll extends DynSearchArgs {

  private def qOptField = SioEsUtil.StdFns.FIELD_ALL

  /** Произвольный текстовый запрос, если есть. */
  def qOpt: Option[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    // TODO Для коротких запросов следует искать по receiverId и фильтровать по qStr (query-filter + match-query).
    TextQuerySearch.mkEsQuery(qOptField, qOpt, super.toEsQueryOpt)
  }

  override def sbInitSize: Int = {
    collStringSize(qOpt, super.sbInitSize, addOffset = qOptField.length + 1)
  }

  override def toStringBuilder: StringBuilder = {
    // Для форматирования вывода используется эта функция. Выводит в lucene формате: field:value.
    val qOptWithFn = qOpt.map(qOptField + ":" + _)
    fmtColl2sb("qStr", qOptWithFn, super.toStringBuilder)
  }

}


trait FtsAllDflt extends FtsAll {
  override def qOpt: Option[String] = None
}
