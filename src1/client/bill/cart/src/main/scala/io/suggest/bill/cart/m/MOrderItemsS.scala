package io.suggest.bill.cart.m

import diode.FastEq
import diode.data.Pot
import io.suggest.bill.cart.MOrderContent
import io.suggest.jd.render.v.JdCss
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 15:07
  * Description: Контейнер разных данных по текущему ордеру или корзине.
  */
object MOrderItemsS {

  implicit object MOrderItemsSFastEq extends FastEq[MOrderItemsS] {
    override def eqv(a: MOrderItemsS, b: MOrderItemsS): Boolean = {
      (a.orderContents ===* b.orderContents) &&
        (a.itemsSelected ===* b.itemsSelected) &&
        (a.jdCss ===* b.jdCss)
    }
  }

  @inline implicit def univEq: UnivEq[MOrderItemsS] = UnivEq.derive

}


/** Данные биллинга.
  *
  * @param orderContents Содержимое ордера.
  */
case class MOrderItemsS(
                         orderContents    : Pot[MOrderContent]    = Pot.empty,
                         itemsSelected    : Set[Gid_t]            = Set.empty,
                         jdCss            : JdCss,
                       ) {

  def withOrderContents(orderContents: Pot[MOrderContent]) = copy(orderContents = orderContents)
  def withItemsSelected( itemsSelected: Set[Gid_t] ) = copy( itemsSelected = itemsSelected )
  def withJdCss(jdCss: JdCss) = copy(jdCss = jdCss)

}
