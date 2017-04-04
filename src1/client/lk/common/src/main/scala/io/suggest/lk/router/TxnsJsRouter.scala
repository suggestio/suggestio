package io.suggest.lk.router

import io.suggest.sjs.common.model.Route

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.15 12:26
 * Description: js-router для обращения к серверу за новыми данными транзакций.
 */
@js.native
sealed trait TxnLkBillingCtl extends js.Object {

  def txnsList(adnId: String, page: Int, inline: Boolean): Route = js.native

  /** Получить HTML-содержимое окошка информации. */
  def _rcvrInfoWndBody(nodeId: String, forAdId: String = null): Route = js.native

}
