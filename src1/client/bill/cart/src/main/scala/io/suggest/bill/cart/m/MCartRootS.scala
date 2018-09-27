package io.suggest.bill.cart.m

import diode.FastEq
import io.suggest.bill.cart.MCartConf
import io.suggest.ueq.UnivEqUtil._
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
      (a.conf ===* b.conf) &&
      (a.order ===* b.order)
    }
  }

  implicit def univEq: UnivEq[MCartRootS] = UnivEq.derive

}


/**
  * Корневая модель состояния корзины товаров и услуг.
  *
  * @param order Текущие данные заказа, полученные с сервера.
  * @param conf Данные конфигурации компонента, необходимые для запросов к серверу.
  */
case class MCartRootS(
                       conf        : MCartConf,
                       order       : MOrderItemsS,
                     ) {

  def withConf(conf: MCartConf) = copy(conf = conf)
  def withOrder(order: MOrderItemsS) = copy(order = order)

}
