package io.suggest.bill.cart

import com.softwaremill.macwire._
import io.suggest.bill.cart.v.itm.{ItemRowR, ItemsTableBodyR, ItemsTableHeadR, ItemsToolBarR}
import io.suggest.bill.cart.v.order._
import io.suggest.bill.cart.v.txn.TxnsR
import io.suggest.jd.render.JdRenderModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 18:21
  * Description: Модули для компонентов.
  */
class CartModules {

  import JdRenderModule._

  lazy val cartPageCircuit = wire[CartPageCircuit]

  lazy val orderR = wire[OrderR]
  lazy val cartCss = wire[OrderCss]

  lazy val itemRowR = wire[ItemRowR]
  lazy val itemTableHeadR = wire[ItemsTableHeadR]
  lazy val itemTableBodyR = wire[ItemsTableBodyR]
  lazy val itemsToolBarR = wire[ItemsToolBarR]
  lazy val goToPayBtnR = wire[GoToPayBtnR]
  lazy val orderInfoR = wire[OrderInfoR]
  lazy val txnsR = wire[TxnsR]

}
