package models.mbill

import io.suggest.bill.{MCurrency, MPrice}
import io.suggest.mbill2.m.balance.MBalance
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.order.MOrder
import io.suggest.mbill2.m.txn.MTxn

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.17 15:44
  * Description: Модель аргументов для рендера шаблона [[views.html.lk.billing.order.ShowOrderTpl]].
  *
  * @param morder Просматриваемый ордер.
  * @param txns Транзакции, связанные с этим ордером.
  * @param payOverPrices Карта платежных оверпрайсов по валютам.
  *                      Оверпрайсинг -- когда юзер оплатил больше, чем стоит заказ.
  *                      Такое бывает, если стоимость заказа меньше размера минимального платежа.
  */
case class MShowOrderTplArgs(
                              override val _underlying    : IItemsTplArgs,
                              morder                      : MOrder,
                              orderPrices                 : Seq[MPrice],
                              override val txns           : Seq[MTxn],
                              override val balances       : Map[Gid_t, MBalance],
                              payOverPrices               : Map[MCurrency, MPrice]
                            )
  extends ILkTxnsListTplArgs
  with IItemsTplArgsWrap

