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

  @inline implicit def univEq[T: UnivEq]: UnivEq[MEsNestedSearch[T]] = UnivEq.force

}

case class MEsNestedSearch[T](
                               clauses        : Seq[T]                        = Nil,
                               innerHits      : Option[InnerHitBuilder]       = None,
                             )
