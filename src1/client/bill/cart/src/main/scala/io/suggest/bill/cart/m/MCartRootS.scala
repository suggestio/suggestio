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
      (a.order ===* b.order) &&
      (a.pay ===* b.pay)
    }
  }

  @inline implicit def univEq: UnivEq[MCartRootS] = UnivEq.derive

  def conf = GenLens[MCartRootS](_.conf)
  def order = GenLens[MCartRootS](_.order)
  def pay = GenLens[MCartRootS](_.pay)

}


/**
  * Cart Form's root state model class.
  *
  * @param order Current order data/state. Mostly received from server.
  * @param conf Form configuration.
  * @param pay In-cart payment forms state.
  */
case class MCartRootS(
                       conf        : MCartConf,
                       order       : MOrderItemsS,
                       pay         : MCartPayS          = MCartPayS.empty,
                     )
