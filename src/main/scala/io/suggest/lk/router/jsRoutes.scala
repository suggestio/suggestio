package io.suggest.lk.router

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 17:43
 * Description: Глобальный интерфейс для js-роутеров, зарегистрированных под именем "jsRoutes".
 */
@js.native
@JSName("jsRoutes")
object jsRoutes extends js.Object {

  def controllers: Controllers = js.native

}


@js.native
sealed trait Controllers extends js.Object {

  def LkBill2: TxnLkBillingCtl = js.native

  def MarketAd: MarketAdFormCtl = js.native

  def LkAdvGeo: LkAdvGeoCtl = js.native

}
