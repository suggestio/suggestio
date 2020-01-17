package io.suggest.n2.edge.edit.m

import diode.FastEq
import diode.data.Pot
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.2020 21:38
  * Description: Состояние диалога подтверждения удаления.
  */
object MDeleteDiaS {

  def empty = apply()

  implicit object MDeleteDiaSFastEq extends FastEq[MDeleteDiaS] {
    override def eqv(a: MDeleteDiaS, b: MDeleteDiaS): Boolean = {
      (a.opened ==* b.opened) &&
      (a.deleteReq ===* b.deleteReq)
    }
  }

  @inline implicit def univEq: UnivEq[MDeleteDiaS] = UnivEq.derive

  val opened      = GenLens[MDeleteDiaS]( _.opened )
  val deleteReq   = GenLens[MDeleteDiaS]( _.deleteReq )

}


case class MDeleteDiaS(
                        opened          : Boolean             = false,
                        deleteReq       : Pot[None.type]      = Pot.empty,
                      )
