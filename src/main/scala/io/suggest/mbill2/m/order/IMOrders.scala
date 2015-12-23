package io.suggest.mbill2.m.order

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.12.15 16:32
 * Description: Интерфейс с полем DI-инстанса [[MOrders]].
 */
trait IMOrders {

  /** DI-Инстанс [[MOrder]]. */
  def mOrders: MOrders

}
