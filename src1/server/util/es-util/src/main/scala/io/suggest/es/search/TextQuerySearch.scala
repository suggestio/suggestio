package io.suggest.es.search

import io.suggest.text.util.TextQueryV2Util
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.12.14 22:40
 * Description: Утиль для написания поисковых аддоннов для опционального полнотекстового поиска.
 */
object TextQuerySearch {

  /**
   * Код сборки поискового запроса вынесен в статику, т.к.
   * @param qOpt Поисковый запрос, если есть.
   * @param fn Название поля с индексом на стороне ES.
   * @param qbOpt0 Исходное значение super.toEsQueryOpt().
   * @return Новое возвращаемое значение toEsQueryOpt.
   */
  def mkEsQuery(fn: String, qOpt: Option[String], qbOpt0: Option[QueryBuilder]): Option[QueryBuilder] = {
    // Собираем запрос текстового поиска.
    val ftsQueryOpt = qOpt.flatMap[QueryBuilder] { q =>
      TextQueryV2Util.queryStr2QueryMarket(q, fn)
        .map { _.q }
    }
    // Навешиваем поисковый запрос на исходный query-builder.
    qbOpt0
      .map { qb0 =>
        ftsQueryOpt.fold(qb0) { ftsQuery =>
          QueryBuilders.boolQuery()
            .must(qb0)
            .filter(ftsQuery)
        }
      }
      .orElse {
        ftsQueryOpt
      }
  }

}
