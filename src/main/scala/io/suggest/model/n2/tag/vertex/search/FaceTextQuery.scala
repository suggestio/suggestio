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
trait FaceTextQuery extends DynSearchArgs {

  /** match-содержимое для матчинга по имени тега. */
  def tagVxFace: Option[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qb0Opt = super.toEsQueryOpt
    val tvfOpt = tagVxFace
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

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("tagVxFace", tagVxFace, super.toStringBuilder)
  }
  override def sbInitSize: Int = {
    val l1 = super.sbInitSize
    val tvf = tagVxFace
    if (tvf.nonEmpty) {
      l1 + 20 + tvf.get.length
    } else {
      l1
    }
  }

}


/** Дефолтовая реализация [[FaceTextQuery]]. */
trait FaceTextQueryDflt extends FaceTextQuery {
  override def tagVxFace: Option[String] = None
}


/** Враппер для реализаций [[FaceTextQuery]]. */
trait FaceTextQueryWrap extends FaceTextQuery with DynSearchArgsWrapper {
  override type WT <: FaceTextQuery
  override def tagVxFace = _dsArgsUnderlying.tagVxFace
}
