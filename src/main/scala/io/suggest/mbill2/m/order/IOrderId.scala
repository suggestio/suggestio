package io.suggest.mbill2.m.order

import io.suggest.mbill2.m.gid.Gid_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 12:38
 * Description: Интерфейс к id связанного ордера.
 */
trait IOrderId {

  def orderId: Gid_t

}
