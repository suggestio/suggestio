package io.suggest.bill.cart.m

import diode.FastEq
import io.suggest.bill.cart.MCartConf
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 16:05
  * Description: Cart Form's root state model - statics.
  */
object MCartRootS {

  implicit object MCartRootSFastEq extends FastEq[MCartRootS] {
    override def eqv(a: MCartRootS, b: MCartRootS): Boolean = {
      (a.conf ===* b.conf) &&
      (a.order ===* b.order)
    }
  }

  @inline implicit def univEq: UnivEq[MCartRootS] = UnivEq.derive

  val conf = GenLens[MCartRootS](_.conf)
  val order = GenLens[MCartRootS](_.order)

}


/**
  * Cart Form's root state model class.
  *
  * @param order Current order data/state. Mostly received from server.
  * @param conf Form configuration.
  */
case class MCartRootS(
                       conf        : MCartConf,
                       order       : MOrderItemsS,
                     )
