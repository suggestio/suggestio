package io.suggest.lk.adv.direct.fsm.states

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.15 13:52
 * Description: FSM-аддон для поддержки FSM-состояния ожидания действий юзера.
 */
trait StandBy extends IntervalSignals with NodesSignals {

  /** Трейт для сборки состояния ожидания действию юзера без прочих особенностей. */
  protected[this] trait StandByStateT
    extends PeriodSignalsStateT
    with NodesSignalsStateT
  {
  }

}
