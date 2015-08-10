package io.suggest.lk.bill.txn.router

import io.suggest.sjs.common.model.Route

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.15 12:26
 * Description: js-router для обращения к серверу за новыми данными транзакций.
 */
class TxnLkBillingCtl extends js.Object {

  def txnsList(adnId: String, page: Int, inline: Boolean): Route = js.native

}
