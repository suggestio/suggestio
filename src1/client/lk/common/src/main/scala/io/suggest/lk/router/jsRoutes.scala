package io.suggest.lk.router

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import io.suggest.js.JsRoutesConst.GLOBAL_NAME

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 17:43
 * Description: Global-scope интерфейс для js-роутеров.
 */
@js.native
@JSGlobal(GLOBAL_NAME)
object jsRoutes extends js.Object {

  def controllers: Controllers = js.native

}


@js.native
sealed trait Controllers extends js.Object {

  def LkBill2: TxnLkBillingCtl = js.native

  def MarketAd: MarketAdFormCtl = js.native

}
