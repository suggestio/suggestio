package io.suggest.mbill2.m.order

import io.suggest.common.menum.{EnumApply, EnumMaybeWithName}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 17:53
 * Description: Статусы ордеров.
 */
object MOrderStatuses extends EnumMaybeWithName with EnumApply {

  protected[this] class Val(val strId: String)
    extends super.Val(strId)
    with ValT
  {

    /**
     * Можно ли изменять заказ?
     * @return true только на ранних этапах.
     */
    def userCanChangeItems: Boolean = false

  }

  override type T = Val


  /** Черновик заказа, корзина. В т.ч. ожидание платежа. Пока заказ не оплачен, он черновик. */
  val Draft = new Val("a") {
    override def userCanChangeItems = true
  }

  /** Оплата заказа проведена. Дальше всё происходит на уровне item'ов заказа. */
  val Closed = new Val("d")

}
