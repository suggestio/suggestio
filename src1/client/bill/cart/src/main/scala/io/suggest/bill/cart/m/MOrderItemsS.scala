package io.suggest.bill.cart.m

import diode.FastEq
import diode.data.Pot
import io.suggest.bill.cart.MOrderContent
import io.suggest.jd.render.m.MJdRuntime
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

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
      (a.jdRuntime ===* b.jdRuntime)
    }
  }

  @inline implicit def univEq: UnivEq[MOrderItemsS] = UnivEq.derive

  val orderContents = GenLens[MOrderItemsS](_.orderContents)
  val itemsSelected = GenLens[MOrderItemsS](_.itemsSelected)

}


/** Данные биллинга.
  *
  * @param orderContents Содержимое ордера.
  */
case class MOrderItemsS(
                         orderContents    : Pot[MOrderContent]    = Pot.empty,
                         itemsSelected    : Set[Gid_t]            = Set.empty,
                         jdRuntime        : MJdRuntime,
                       )
