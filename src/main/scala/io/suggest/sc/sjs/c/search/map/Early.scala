package io.suggest.sc.sjs.c.search.map

import io.suggest.sjs.common.fsm.IFsmMsg
import io.suggest.sjs.common.msg.ErrorMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.16 17:47
  * Description: Трейт для аддонов отложенных сообщений по карте.
  * Отложенные сообщения закидываются в аккамулятор sd.map.early, и потом отсылка повторяется по команде.
  * Это необходимо, т.к. инициализация карты идёт в неск.этапов и асинхронно относительно других компонентов системы.
  */
trait Early extends MapFsmStub {

  /** Отправлять все непонятные входящие в early-аккамулятор. */
  trait HandleAll2Early extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      // Кто-то швыряется сообщениями карты раньше времени...
      case msg: IFsmMsg =>
        val sd0 = _stateData
        // Ограничиваем макс.длину аккамулятора непринятых сообщений.
        if (sd0.early.size < 5) {
          _stateData = sd0.copy(
            early = msg :: sd0.early
          )
        } else {
          LOG.error( ErrorMsgs.QUEUE_OVERLOADED, msg = msg.toString )
        }
    }
  }


  /** Переслать самому себе все early-сообщения из аккамулятора. */
  trait ApplyAllEarly extends FsmEmptyReceiverState {

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // если в состоянии есть несвоевременные сообщения, то отработать их.
      _sendEarlyMsgs()
    }

  }


  protected[this] def _sendEarlyMsgs() = {
    val sd0 = _stateData
    val earlyMsgs = sd0.early
    if (earlyMsgs.nonEmpty) {
      _stateData = sd0.copy(
        early = Nil
      )
      for (em <- earlyMsgs.reverseIterator) {
        _sendEvent(em)
      }
    }
  }

}
