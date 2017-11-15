package io.suggest.sc.grid.m

import diode.FastEq
import io.suggest.jd.MJdAdData
import io.suggest.jd.tags.JdTag
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq
import japgolly.univeq.UnivEq

import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 19:07
  * Description: Модель рантаймовой инфы по одной рекламной карточке в выдаче.
  * Модель собирается на базе созвучной MJdAdData, более простой по сравнению с этой.
  */
object MScAdData {

  /** Поддержка FastEq для инстансов [[MScAdData]]. */
  implicit object MScAdDataFastEq extends FastEq[MScAdData] {
    override def eqv(a: MScAdData, b: MScAdData): Boolean = {
      (a.template ===* b.template) &&
        (a.edges ===* b.edges)
    }
  }

  implicit def univEq: UnivEq[MScAdData] = UnivEq.derive

  /** Сборка инстанса [[MScAdData]] из инстанса MJdAdData. */
  def apply( jdAdData: MJdAdData ): MScAdData = apply(
    template  = jdAdData.template,
    edges     = jdAdData.edgesMap.mapValues(MEdgeDataJs(_))
  )

}

case class MScAdData(
                      template    : Tree[JdTag],
                      edges       : Map[EdgeUid_t, MEdgeDataJs]
                    )
