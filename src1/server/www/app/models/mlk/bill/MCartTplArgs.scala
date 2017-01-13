package models.mlk.bill

import io.suggest.bill.MGetPriceResp
import io.suggest.mbill2.m.item.MItem
import models.MNode
import models.blk.IRenderArgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 17:07
  * Description: Аргументы для вызова [[views.html.lk.billing.cart.cartTpl]].
  */
trait ICartTplArgs {

  /** Личный кабинет какого узла у нас сейчас? */
  def mnode: MNode

  /** Содержимое ордера. */
  def items: Seq[ICartItem]

  /** return path для возврата из корзины. */
  def r: Option[String]

  /** Итоговая стоимость заказа. */
  def totalPricing: MGetPriceResp

}

/** Дефолтовая реализация модели [[ICartTplArgs]]. */
case class MCartTplArgs(
  override val mnode        : MNode,
  override val items        : Seq[MCartItem],
  override val r            : Option[String],
  override val totalPricing : MGetPriceResp
)
  extends ICartTplArgs


trait ICartItem {

  def mitems: Seq[MItem]

  def brArgs: IRenderArgs

}

case class MCartItem(
  override val mitems   : Seq[MItem],
  override val brArgs   : IRenderArgs
)
  extends ICartItem
