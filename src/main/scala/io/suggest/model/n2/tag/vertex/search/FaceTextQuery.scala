package io.suggest.model.n2.tag.vertex.search

import io.suggest.model.n2.node.MNodeFields
import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.{MatchQueryBuilder, QueryBuilders, FilterBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 14:54
 * Description: Текстовый поиск по фейсам-именам узлов-тегов.
 */
trait FaceTextQuery extends DynSearchArgs {

  /** Критерии поиска тегов. */
  def tagFaces: Seq[ITagFaceCriteria]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qb0Opt = super.toEsQueryOpt

    val tfs = tagFaces
    if (tfs.isEmpty) {
      qb0Opt

    } else {

      // Есть какие-то поисковые критерии. Организовать nested search...
      val fn = MNodeFields.Extras.TAG_FACE_NAME_FN

      // Сконвертить критерии в queries
      val queries: Seq[(QueryBuilder, ITagFaceCriteria)] = {
        for (tf <- tfs) yield {
          val q = QueryBuilders.matchQuery(fn, tf.face)
            // TODO Надо ведь 100% по идее, но не ясно, насколько это ок.
            .minimumShouldMatch( "90%" )
            .operator( MatchQueryBuilder.Operator.AND )
            .`type` {
              if (tf.isPrefix)
                MatchQueryBuilder.Type.PHRASE_PREFIX
              else
                MatchQueryBuilder.Type.PHRASE
            }
            .zeroTermsQuery( MatchQueryBuilder.ZeroTermsQuery.ALL )
          q -> tf
        }
      }

      // Объединить несколько query в одну согласно предикатам.
      val query: QueryBuilder = if (queries.size == 1) {
        queries.head._1
      } else {
        val bq = QueryBuilders.boolQuery()
        var minShouldMatch = 0
        for ((q, tf) <- queries) {
          tf.must match {
            case None =>
              minShouldMatch += 1
              bq.should(q)
            case Some(true) =>
              bq.must(q)
            case Some(false) =>
              bq.mustNot(q)
          }
        }
        bq.minimumNumberShouldMatch(minShouldMatch)
      }

      // Завернуть собранный поиск в nested
      val nestedFn = MNodeFields.Extras.TAG_FACES_FN
      qb0Opt map { qb0 =>
        // Просто полнотекстовый match-поиск по
        val filter = FilterBuilders.nestedFilter(nestedFn, query)
        QueryBuilders.filteredQuery(qb0, filter)
      } orElse {
        val qb2 = QueryBuilders.nestedQuery(nestedFn, query)
        Some(qb2)
      }
    }
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("tagFaces", tagFaces, super.toStringBuilder)
  }

  override def sbInitSize: Int = {
    val l1 = super.sbInitSize
    val tvf = tagFaces
    if (tvf.nonEmpty) {
      l1 + 20 + (30 * tvf.length)
    } else {
      l1
    }
  }

}


/** Дефолтовая реализация [[FaceTextQuery]]. */
trait FaceTextQueryDflt extends FaceTextQuery {
  override def tagFaces: Seq[ITagFaceCriteria] = Nil
}


/** Враппер для реализаций [[FaceTextQuery]]. */
trait FaceTextQueryWrap extends FaceTextQuery with DynSearchArgsWrapper {
  override type WT <: FaceTextQuery
  override def tagFaces = _dsArgsUnderlying.tagFaces
}
