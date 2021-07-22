package io.suggest.es.model

import japgolly.univeq.UnivEq
import org.elasticsearch.index.query.InnerHitBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.03.2020 11:37
  * Description: Модель nested-критериев поиска, служащая обёрткой над критериями и какими-то
  * дополнительными аргументами nested-поиска, не связанными с данными напрямую.
  */
object MEsNestedSearch {

  def empty[T] = apply[T]()

  @inline implicit def univEq[T: UnivEq]: UnivEq[MEsNestedSearch[T]] = UnivEq.derive

  def plain[T](clauses: T*): MEsNestedSearch[T] = {
    MEsNestedSearch(
      clauses = clauses.map( MEsNestedClause(_) )
    )
  }

  def innerHitsIndexed[T](clauses: Seq[T])( mkInnerHitsF: (T, Int) => Option[InnerHitBuilder] ): MEsNestedSearch[T] = {
    MEsNestedSearch(
      clauses = clauses
        .iterator
        .zipWithIndex
        .map { case (cr, i) =>
          MEsNestedClause(
            clause = cr,
            innerHits = mkInnerHitsF(cr, i),
          )
        }
        .toSeq,
    )
  }
  def innerHitsBuildIndexed[T](innerHitsOpt: Option[MEsInnerHitsInfo], clauses: Seq[T]): MEsNestedSearch[T] = {
    innerHitsIndexed( clauses ) { (_, i) =>
      MEsInnerHitsInfo.buildInfoOpt( innerHitsOpt, nameSuffix = Some(i.toString) )
    }
  }

}


/** Container of nested search definitions.
  * @param clauses List of abstract search criterias, somehow merged into query.
  * @tparam T
  */
case class MEsNestedSearch[T](
                               clauses        : Seq[MEsNestedClause[T]]       = Nil,
                             )


object MEsNestedClause {
  @inline implicit def univEq[T: UnivEq]: UnivEq[MEsNestedClause[T]] = UnivEq.force
}

/** Container data for single nested sub-search.
  * @param clause Nested clause to compile into ES QueryBuilder.
  * @param innerHits ES specification of inner hits for current nested clause.
  * @tparam T Type of DSL clause.
  */
case class MEsNestedClause[T](
                               clause         : T,
                               innerHits      : Option[InnerHitBuilder]       = None,
                             )
