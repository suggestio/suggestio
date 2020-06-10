package io.suggest.sc.m.search

import diode.FastEq
import io.suggest.geo.MGeoPoint
import io.suggest.maps.nodes.MGeoNodePropsShapes
import io.suggest.sc.v.search.SearchCss
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.06.2020 14:42
  * Description: Модель пропертисов для NodeFoundR.
  */
object MNodesFoundRowProps {
  implicit object MNodesFoundRowPropsFeq extends FastEq[MNodesFoundRowProps] {
    override def eqv(a: MNodesFoundRowProps, b: MNodesFoundRowProps): Boolean = {
      (a.node                 ===* b.node) &&
      (a.searchCss            ===* b.searchCss) &&
      (a.withDistanceToNull   ===* b.withDistanceToNull) &&
      (a.selected              ==* b.selected)
    }
  }

  @inline implicit def univEq: UnivEq[MNodesFoundRowProps] = UnivEq.derive

}


final case class MNodesFoundRowProps(
                                      node               : MGeoNodePropsShapes,
                                      searchCss          : SearchCss,
                                      withDistanceToNull : MGeoPoint  = null,
                                      selected           : Boolean    = false,
                                    )
