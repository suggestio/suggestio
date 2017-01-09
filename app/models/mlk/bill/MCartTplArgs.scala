package models.mlk.bill

import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.price.MPrice
import models.MNode
import models.adv.price.IAdvPricing
import models.blk.IRenderArgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 17:07
  * Description: Аргументы для вызова [[views.html.lk.billing.order.cartTpl]].
  */
trait ICartTplArgs {

  /** Личный кабинет какого узла у нас сейчас? */
  def mnode: MNode

  /** Содержимое ордера. */
  def items: Seq[ICartItem]

  /** return path для возврата из корзины. */
  def r: Option[String]

  /** Итоговая стоимость заказа. */
  def totalPricing: IAdvPricing


}

/** Дефолтовая реализация модели [[ICartTplArgs]]. */
case class MCartTplArgs(
  override val mnode        : MNode,
  override val items        : Seq[MCartItem],
  override val r            : Option[String],
  override val totalPricing : IAdvPricing
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
