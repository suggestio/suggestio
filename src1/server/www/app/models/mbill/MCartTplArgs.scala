package models.mbill

import io.suggest.bill.MGetPriceResp

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 17:07
  * Description: Модели для рендера корзины и списка item'ов ордера.
  */


/** Модель аргументов рендера [[views.html.lk.billing.order.CartTpl]]. */
trait ICartTplArgs extends IItemsTplArgsWrap {

  /** return path для возврата из корзины. */
  def r: Option[String]

  /** Итоговая стоимость заказа. */
  def totalPricing: MGetPriceResp

}

/** Дефолтовая реализация модели [[ICartTplArgs]]. */
case class MCartTplArgs(
                         override val _underlying   : IItemsTplArgs,
                         override val r             : Option[String],
                         override val totalPricing  : MGetPriceResp
                       )
  extends ICartTplArgs
