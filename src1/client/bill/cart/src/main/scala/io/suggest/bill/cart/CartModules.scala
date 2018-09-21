package io.suggest.bill.cart

import com.softwaremill.macwire._
import io.suggest.bill.cart.v.CartR
import io.suggest.bill.cart.v.order._
import io.suggest.jd.render.JdRenderModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 18:21
  * Description: Модули для компонентов.
  */
class CartModules {

  val jdRenderModule = wire[JdRenderModule]
  import jdRenderModule._

  lazy val cartPageCircuit = wire[CartPageCircuit]

  lazy val cartR = wire[CartR]

  lazy val itemRowR = wire[ItemRowR]
  lazy val itemRowPreviewR = wire[ItemRowPreviewR]
  lazy val itemTableHeadR = wire[ItemsTableHeadR]
  lazy val itemTableBodyR = wire[ItemsTableBodyR]

}
