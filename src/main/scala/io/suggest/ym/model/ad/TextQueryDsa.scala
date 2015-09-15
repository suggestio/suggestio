package io.suggest.ym.model.ad

import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import io.suggest.util.SioConstants
import io.suggest.util.text.TextQueryV2Util
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.12.14 22:40
 * Description: Поисковый аддон для опционального текстового поиска.
 */
trait TextQueryDsa extends DynSearchArgs {

  /** Произвольный текстовый запрос, если есть. */
  def qOpt: Option[String]

  def qOptField: String = SioConstants.FIELD_ALL

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val ftsQueryOpt = qOpt.flatMap[QueryBuilder] { q =>
      // Собираем запрос текстового поиска.
      // TODO Для коротких запросов следует искать по receiverId и фильтровать по qStr (query-filter + match-query).
      TextQueryV2Util.queryStr2QueryMarket(q, qOptField)
        .map { _.q }
    }
    super.toEsQueryOpt
      .flatMap { qb0 =>
        ftsQueryOpt map { ftsQuery =>
          val filter = FilterBuilders.queryFilter(ftsQuery)
          QueryBuilders.filteredQuery(qb0, filter)
        }
      }
      .orElse {
        ftsQueryOpt
      }
  }

  /** Для форматирования вывода используется эта функция. Выводит в lucene формате: field:value. */
  protected def qOptWithFn = qOpt.map(qOptField + ":" + _)

  override def sbInitSize: Int = {
    collStringSize(qOpt, super.sbInitSize, addOffset = qOptField.length + 1)
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("qStr", qOptWithFn, super.toStringBuilder)
  }

}


trait TextQueryDsaDflt extends TextQueryDsa {
  override def qOpt: Option[String] = None
}


trait TextQueryDsaWrapper extends TextQueryDsa with DynSearchArgsWrapper {
  override type WT <: TextQueryDsa
  override def qOpt = _dsArgsUnderlying.qOpt
}
