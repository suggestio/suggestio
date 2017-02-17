package models.mbill

import io.suggest.bill.MPrice
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.order.MOrder
import io.suggest.model.n2.node.MNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.17 22:38
  * Description: Модель аргументов для шаблона [[views.html.lk.billing.pay.OrdersTpl]].
  */
case class MOrdersTplArgs(
                           mnode          : MNode,
                           orders         : Seq[MOrder],
                           prices         : Map[Gid_t, Iterable[MPrice]],
                           cartOrderId    : Option[Gid_t],
                           ordersTotal    : Int,
                           page           : Int,
                           ordersPerPage  : Int
                         )
