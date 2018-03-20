package io.suggest.sc.m.menu

import diode.FastEq
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 21:45
  * Description: Модель состояния левой панели меню.
  */
object MMenuS {

  def default = MMenuS()

  implicit object MMenuSFastEq extends FastEq[MMenuS] {
    override def eqv(a: MMenuS, b: MMenuS): Boolean = {
      a.opened ==* b.opened
    }
  }

  implicit def univEq: UnivEq[MMenuS] = UnivEq.derive

}


case class MMenuS(
                   opened: Boolean = false
                 ) {

  def withOpened(opened: Boolean) = copy(opened = opened)

}