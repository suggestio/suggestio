package models.mbill

import io.suggest.bill.MPrice
import io.suggest.mbill2.m.balance.MBalance
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.order.MOrder
import io.suggest.mbill2.m.txn.MTxn
import io.suggest.model.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.17 15:44
  * Description: Модель аргументов для рендера шаблона [[views.html.lk.billing.order.ShowOrderTpl]].
  *
  * @param morder Просматриваемый ордер.
  * @param txns Транзакции, связанные с этим ордером.
  */
case class MShowOrderTplArgs(
                              override val _underlying    : IItemsTplArgs,
                              morder                      : MOrder,
                              orderPrices                 : Seq[MPrice],
                              override val txns           : Seq[MTxn],
                              override val balances       : Map[Gid_t, MBalance]
                            )
  extends ILkTxnsListTplArgs
  with IItemsTplArgsWrap

