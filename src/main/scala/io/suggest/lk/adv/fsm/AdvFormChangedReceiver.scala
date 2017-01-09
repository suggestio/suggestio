package io.suggest.lk.adv.fsm

import io.suggest.lk.adv.m.IAdvFormChanged
import io.suggest.sjs.common.fsm.SjsFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.16 16:46
  * Description: Ресивер сигналов обновления формы.
  */
trait AdvFormChangedReceiver extends SjsFsm with IUpdatePriceDataStart {

  /** Подмешать это в FSM-состояние, чтобы в ресивере появился вызов  */
  trait AdvFormChangedReceiverStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      case msg: IAdvFormChanged =>
        _handleAdvFormChanged(msg)
    }

    /** Реакция на получение сообщений об изменении формы.
      * Изначально -- вызывать апдейт серверных данных формы. */
    protected def _handleAdvFormChanged(msg: IAdvFormChanged): Unit = {
      _upStart()
    }
  }

}
