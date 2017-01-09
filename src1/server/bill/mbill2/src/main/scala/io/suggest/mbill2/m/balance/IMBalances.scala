package io.suggest.mbill2.m.balance

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.12.15 15:43
 * Description: Интерфейс для доступа к DI-экземпляру [[MBalances]].
 */
trait IMBalances {

  def mBalances: MBalances

}
