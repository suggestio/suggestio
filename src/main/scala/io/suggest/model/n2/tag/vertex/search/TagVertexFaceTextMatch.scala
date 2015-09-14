package io.suggest.model.n2.tag.vertex.search

import io.suggest.model.n2.tag.vertex.{MTagVertex, MTagFace}
import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.{MatchQueryBuilder, QueryBuilders, FilterBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 14:54
 * Description: Текстовый поиск по фейсам-именам узлов-тегов.
 */
trait TagVertexFaceTextMatch extends DynSearchArgs {

  /** match-содержимое для матчинга по имени тега. */
  def tagVertexFaceNameMatch: Option[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qb0Opt = super.toEsQueryOpt
    val tvfOpt = tagVertexFaceNameMatch
    if (tvfOpt.isEmpty) {
      qb0Opt

    } else {
      val tvf = tvfOpt.get
      // Сборка nested-запроса.
      val ftsQb = QueryBuilders.matchQuery(MTagFace.NAME_ESFN, tvf)
        // TODO Надо ведь 100% по идее, но не ясно, насколько это ок.
        .minimumShouldMatch( "90%" )
        .operator( MatchQueryBuilder.Operator.AND )

      // Добавить nested-query к существующей query или создать новую.
      qb0Opt map { qb0 =>
        // Просто полнотекстовый match-поиск по
        val filter = FilterBuilders.nestedFilter(MTagVertex.FACES_ESFN, ftsQb)
        QueryBuilders.filteredQuery(qb0, filter)

      } orElse {
        val qb2 = QueryBuilders.nestedQuery(MTagVertex.FACES_ESFN, ftsQb)
        Some(qb2)
      }
    }
  }

}


/** Дефолтовая реализация [[TagVertexFaceTextMatch]]. */
trait TagVertexFaceTextMatchDflt extends TagVertexFaceTextMatch {
  override def tagVertexFaceNameMatch: Option[String] = None
}


/** Враппер для реализаций [[TagVertexFaceTextMatch]]. */
trait TagVertexFaceTextMatchWrap extends TagVertexFaceTextMatch with DynSearchArgsWrapper {
  override type WT <: TagVertexFaceTextMatch
  override def tagVertexFaceNameMatch = _dsArgsUnderlying.tagVertexFaceNameMatch
}
