package io.suggest.lk.router

import io.suggest.lk.ad.form.router.MarketAdFormCtl
import io.suggest.lk.bill.txn.router.TxnLkBillingCtl

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 17:43
 * Description: Глобальный интерфейс для js-роутеров, зарегистрированных под именем "jsRoutes".
 */
@JSName("jsRoutes")
object jsRoutes extends js.Object {

  def controllers: Controllers = js.native

}


class Controllers extends js.Object {

  def MarketLkBilling: TxnLkBillingCtl = js.native

  def MarketAd: MarketAdFormCtl = js.native
}