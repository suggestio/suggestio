package io.suggest.bill.cart

import com.softwaremill.macwire._
import io.suggest.bill.cart.v.CartR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.09.18 18:21
  * Description: Модули для компонентов.
  */
class CartModules {

  lazy val cartR = wire[CartR]

}
