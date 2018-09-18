package io.suggest.bill.cart.m

import diode.FastEq
import diode.data.Pot
import io.suggest.bill.cart.MOrderContent
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 16:05
  * Description: Корневая модель состояния компонента полноценной корзины.
  */
object MCartRootS {

  implicit object MCartRootSFastEq extends FastEq[MCartRootS] {
    override def eqv(a: MCartRootS, b: MCartRootS): Boolean = {
      a.content ===* b.content
    }
  }

  implicit def univEq: UnivEq[MCartRootS] = UnivEq.derive

}


/**
  * Корневая модель состояния корзины товаров и услуг.
  * @param content Текущее наполнение заказа.
  */
case class MCartRootS(
                       content    : Pot[MOrderContent]    = Pot.empty
                     )
