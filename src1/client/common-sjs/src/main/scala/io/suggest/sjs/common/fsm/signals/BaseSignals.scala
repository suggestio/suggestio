package io.suggest.sjs.common.fsm.signals

import io.suggest.sjs.common.fsm.IFsmMsg

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.16 15:39
  * Description: Файл с разными очень базовыми сигналами между различными FSM.
  */

/** Интерфейс для сигналов об изменении отображенности на экране текущей вкладки/приложения. */
trait IVisibilityChangeSignal extends IFsmMsg {

  /** На какое именно состояние изменилась видимость текущего приложения?
    * @return true  - теперь скрыто
    *         false - теперь видимо.
    */
  def isHidden: Boolean

}

