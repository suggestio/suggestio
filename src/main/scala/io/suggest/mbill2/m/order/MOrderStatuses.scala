package io.suggest.mbill2.m.order

import io.suggest.common.menum.EnumMaybeWithName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.15 17:53
 * Description: Статусы ордеров.
 */
object MOrderStatuses extends EnumMaybeWithName {

  protected[this] class Val(val strId: String)
    extends super.Val(strId)
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

  /** Оплата прошла, пошло исполнение подзаказов (item'ов). */
  val AwaitItemStatuses = new Val("c")

  /** Заказ завершён. */
  val Closed = new Val("d")

  def withNameT(n: String): T = {
    withName(n)
  }
  def unapply(v: T): Option[String] = {
    Some(v.strId)
  }

}
