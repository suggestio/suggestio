package io.suggest.lk.adv.geo.tags.fsm.states

import io.suggest.lk.adv.geo.tags.fsm.AgtFormFsmStub
import io.suggest.lk.adv.geo.tags.m.signal.RcvrChanged

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.16 22:05
  * Description: Аддон для добавления поддержки обработки сигналов от галочек-ресиверов.
  */
trait Rcvrs extends AgtFormFsmStub {

  trait RcvrsStateT extends FsmEmptyReceiverState {
    override def receiverPart: Receive = {
      case rnc: RcvrChanged =>
        _handleRcvrChanged(rnc)
    }

    protected def _handleRcvrChanged(rnc: RcvrChanged): Unit = {
      LOG.warn( msg = "receiver changed: " + rnc )
      // TODO Реализовать копирование/перезапись изменившихся полей в основную форму. Для этого нужны id'шники и прочее.
    }

  }

}
