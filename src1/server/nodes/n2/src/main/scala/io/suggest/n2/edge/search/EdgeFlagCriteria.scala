package io.suggest.n2.edge.search

import io.suggest.n2.edge.MEdgeFlag
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.07.2020 22:34
  * Description: Критерий поиска/фильтрации по флагу эджа.
  */
object EdgeFlagCriteria {

  @inline implicit def univEq: UnivEq[EdgeFlagCriteria] = UnivEq.derive

}


/** Контейнер критерия поиска/фильтрации по флагу.
  *
  * @param flag Флаги.
  */
case class EdgeFlagCriteria(
                             flag: Seq[MEdgeFlag] = Nil,
                           )

