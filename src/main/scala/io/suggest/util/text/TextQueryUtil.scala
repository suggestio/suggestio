package io.suggest.util.text

import org.elasticsearch.index.query.{QueryBuilders, QueryBuilder}
import io.suggest.util.SioConstants._
import org.elasticsearch.common.unit.Fuzziness

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.14 10:29
 * Description: Утиль для работы с текстовыми запросами пользователей.
 */
object TextQueryUtil {

  // Регэксп разбиения строки перед последним словом.
  val splitLastWordRe = "\\s+(?=\\S*+$)".r
  val endsWithSpace = "\\s$".r.pattern


  def splitQueryStr(queryStr:String) : Option[(String, String)] = {
    splitLastWordRe.split(queryStr) match {
      case Array() =>
        None

      case Array(words) =>
        // Нужно проверить words на наличие \\s внутри. Если есть, то это fts words. Иначе, engram-часть.
        val result = if (endsWithSpace.matcher(queryStr).find()) {
          (words, "")
        } else {
          ("", words)
        }
        Some(result)

      case Array(fts, ngram:String) =>
        // Бывает, что fts-часть состоит из одного предлога или слова, которое кажется сейчас предлогом.
        // Если отправить в ES в качестве запроса предлог, то, очевидно, будет ноль результатов на выходе.
        val fts1 = if (Stopwords.ALL_STOPS contains fts) {
          ""
        } else {
          fts
        }
        Some((fts1, ngram))
    }
  }

}


/** Утиль для обычного suggest.io, унаследованного от erlang-версии. */
object TextQueryV1Util {

  val FIELDS_ONLY_TITLE = List(FIELD_TITLE)
  val FIELDS_TEXT_ALL   = List(FIELD_TITLE, FIELD_CONTENT_TEXT)

  /**
   * Взять queryString, вбитую юзером, распилить на куски, проанализировать и сгенерить запрос или комбинацию запросов
   * на её основе.
   * @param queryStr Строка, которую набирает в поиске юзер.
   */
  def queryStr2Query(queryStr: String) : Option[QueryBuilder] = {
    // Дробим исходный запрос на куски
    val topQueriesOpt = TextQueryUtil.splitQueryStr(queryStr).map { case (ftsQS, engramQS) =>

      val ftsLen = ftsQS.length
      val engramLen = engramQS.length

      // Отрабатываем edge-ngram часть запроса.
      var queries : List[QueryBuilder] = if (engramLen == 0) {
        Nil

      } else {
        // Если запрос короткий, то искать только по title
        val fields = if (ftsLen + engramLen <= 1)
          FIELDS_ONLY_TITLE
        else
          FIELDS_TEXT_ALL
        // Генерим базовый engram-запрос
        var queries1 = fields.map { _field =>
          val _subfield = subfield(_field, SUBFIELD_ENGRAM)
          QueryBuilders.matchQuery(_subfield, engramQS)
        }
        // Если чел уже набрал достаточное кол-во символов, то искать парралельно в fts
        if (engramLen >= 4) {
          val ftsQuery = QueryBuilders.matchQuery(FIELD_ALL, engramQS)
          queries1 = ftsQuery :: queries1
        }
        // Если получилось несколько запросов, то обернуть их в bool-query
        val finalEngramQuery = if (queries1.tail == Nil) {
          queries1.head
        } else {
          val minShouldMatch = 1
          val boolQB = QueryBuilders.boolQuery().minimumNumberShouldMatch(minShouldMatch)
          queries1.foreach { boolQB.should }
          boolQB
        }
        List(finalEngramQuery)
      }

      // Обработать fts-часть исходного запроса.
      if (ftsLen > 1) {
        val queryFts = QueryBuilders.matchQuery(FIELD_ALL, ftsQS)
        queries = queryFts :: queries
      }

      queries
    }

    // Если получилось несколько запросов верхнего уровня, то обернуть их bool-query must
    topQueriesOpt match {
      case None => None

      case Some(topQueries) =>
        topQueries match {

          case List(query) => Some(query)
          case Nil => None

          case _ =>
            val queryBool = QueryBuilders.boolQuery()
            topQueries.foreach { queryBool.must }
            Some(queryBool)
        }
    }
  }

  /** Генератор имён под-полей в multi-field. */
  def subfield(field:String, subfield:String) = field + "." + subfield
}


/** Утиль для v2 поиска в маркете. */
object TextQueryV2Util {

  /**
   * Взять queryString, вбитую юзером, распилить на куски, проанализировать и сгенерить запрос или комбинацию запросов
   * на её основе. Версия для новых индексов в маркете, которые содержат ngram в _all.
   * @param queryStr Строка, которую набирает в поиске юзер.
   */
  def queryStr2QueryMarket(queryStr: String) : Option[MarketTextQuery] = {
    // Дробим исходный запрос на куски
    val topQueriesOpt = TextQueryUtil.splitQueryStr(queryStr).map { case (ftsQS, engramQS) =>
      val ftsLen = ftsQS.length
      val engramLen = engramQS.length
      val isDifficult = ftsLen == 0 && engramLen <= 2

      // Отрабатываем edge-ngram часть запроса.
      var queries : List[QueryBuilder] = if (engramLen == 0) {
        Nil
      } else {
        // Генерим базовый engram-запрос
        var queries1 = List(
          QueryBuilders.matchQuery(FIELD_ALL, engramQS)
            .analyzer(MINIMAL_AN)
            .fuzziness(Fuzziness.AUTO)
        )
        // Если чел уже набрал достаточное кол-во символов, то искать парралельно в fts
        if (engramLen >= 4) {
          val ftsQuery = QueryBuilders.matchQuery(FIELD_ALL, engramQS)
            .fuzziness(Fuzziness.AUTO)
          queries1 = ftsQuery :: queries1
        }
        // Если получилось несколько запросов, то обернуть их в bool-query
        val finalEngramQuery = if (queries1.tail == Nil) {
          queries1.head
        } else {
          val minShouldMatch = 1
          val boolQB = QueryBuilders.boolQuery().minimumNumberShouldMatch(minShouldMatch)
          queries1.foreach { boolQB.should }
          boolQB
        }
        List(finalEngramQuery)
      }

      // Обработать fts-часть исходного запроса.
      if (ftsLen > 1) {
        val queryFts = QueryBuilders.matchQuery(FIELD_ALL, ftsQS)
          .fuzziness(Fuzziness.AUTO)
        queries ::= queryFts
      }

      isDifficult -> queries
    }

    // Если получилось несколько запросов верхнего уровня, то обернуть их bool-query must
    topQueriesOpt match {
      case Some((isDifficult, topQueries)) =>
        topQueries match {
          case List(query) =>
            Some(MarketTextQuery(query, isDifficult))
          case Nil =>
            None
          case _ =>
            val queryBool = QueryBuilders.boolQuery()
            topQueries.foreach { queryBool.must }
            Some(MarketTextQuery(queryBool, isDifficult))
        }

      case None => None
    }
  }

}

case class MarketTextQuery(q: QueryBuilder, isDifficult: Boolean)
