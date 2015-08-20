package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.c.scfsm.ScFsmStub

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.15 18:50
 * Description: Аддон для ScFsm для поддержки touch-навигации в focused-выдаче.
 */
trait OnTouch extends ScFsmStub {

  /** Трейт реализации состояния touch-навигации. */
  protected trait FocOnTouchStateT extends FsmState {
  }

}
