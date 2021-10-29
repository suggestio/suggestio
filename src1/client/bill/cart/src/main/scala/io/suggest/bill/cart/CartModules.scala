package io.suggest.bill.cart

import com.softwaremill.macwire._
import io.suggest.bill.cart.v.itm.{ItemRowR, ItemsTableBodyR, ItemsTableHeadR, ItemsDeleteSelectedR}
import io.suggest.bill.cart.v.order._
import io.suggest.bill.cart.v.pay.{CartPayR, PayButtonR, PaySystemScriptR}
import io.suggest.bill.cart.v.pay.systems.YooKassaCartR
import io.suggest.bill.cart.v.txn.TxnsR
import io.suggest.jd.render.JdRenderModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 18:21
  * Description: DI-linking for Cart modules.
  */
class CartModules {

  import JdRenderModule._
  import io.suggest.ReactCommonModule._
  import io.suggest.lk.LkCommonModule._

  lazy val cartPageCircuit = wire[CartPageCircuit]

  lazy val orderR = wire[OrderR]
  lazy val cartCss = wire[OrderCss]

  lazy val itemRowR = wire[ItemRowR]
  lazy val itemTableHeadR = wire[ItemsTableHeadR]
  lazy val itemTableBodyR = wire[ItemsTableBodyR]
  lazy val itemsToolBarR = wire[ItemsDeleteSelectedR]
  lazy val orderInfoR = wire[OrderInfoR]
  lazy val unholdOrderDiaR = wire[UnholdOrderDiaR]

  lazy val txnsR = wire[TxnsR]

  lazy val goToPayBtnR = wire[PayButtonR]
  lazy val paySystemScriptR = wire[PaySystemScriptR]
  lazy val yooKassaCartR = wire[YooKassaCartR]
  lazy val cartPayR = wire[CartPayR]

}
