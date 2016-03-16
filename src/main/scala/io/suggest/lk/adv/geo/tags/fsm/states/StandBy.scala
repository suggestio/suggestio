package io.suggest.lk.adv.geo.tags.fsm.states

import io.suggest.lk.adv.geo.tags.fsm.AgtFormFsmStub
import io.suggest.lk.adv.geo.tags.m.signal.IFormChanged

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 12:36
  * Description: Аддон для Agt FSM для добавления поддержки пассивного ожидания событий.
  */
trait StandBy extends AgtFormFsmStub {

  trait StandByStateT extends FsmEmptyReceiverState {
    override def receiverPart: Receive = super.receiverPart orElse {
      case _: IFormChanged =>
        // Необходимо запустить обовления ценника.
        _upStart()
    }
  }

}
