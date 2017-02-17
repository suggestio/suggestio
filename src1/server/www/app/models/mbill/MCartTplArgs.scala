package models.mbill

import io.suggest.bill.MGetPriceResp
import io.suggest.mbill2.m.item.MItem
import models.MNode
import models.blk.IRenderArgs
import models.im.MImgT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 17:07
  * Description: Модели для рендера корзины и списка item'ов ордера.
  */


/** Модель аргументов рендера [[views.html.lk.billing.order.CartTpl]]. */
trait ICartTplArgs extends IItemsTplArgs{

  /** Личный кабинет какого узла у нас сейчас? */
  def mnode: MNode

  /** return path для возврата из корзины. */
  def r: Option[String]

  /** Итоговая стоимость заказа. */
  def totalPricing: MGetPriceResp

}

/** Дефолтовая реализация модели [[ICartTplArgs]]. */
case class MCartTplArgs(
                         override val mnode         : MNode,
                         override val itemNodes     : Seq[MNode],
                         override val node2logo     : Map[String, MImgT],
                         override val node2brArgs   : Map[String, IRenderArgs],
                         override val node2items    : Map[String, Seq[MItem]],
                         override val r             : Option[String],
                         override val totalPricing  : MGetPriceResp
                       )
  extends ICartTplArgs
