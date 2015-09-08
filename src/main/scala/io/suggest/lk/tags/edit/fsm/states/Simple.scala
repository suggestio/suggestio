package io.suggest.lk.tags.edit.fsm.states

import io.suggest.lk.tags.edit.fsm.TagsEditFsmStub
import io.suggest.lk.tags.edit.m.signals.AddBtnClick

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 16:29
 * Description: Аддон для [[io.suggest.lk.tags.edit.fsm.TagsEditFsm]] для реализации примитивной
 * работы без участия сервера.
 * Создан как заглушка во избежание написания кучи кода на стороне сервера и на клиенте.
 */
trait Simple extends TagsEditFsmStub {

  protected trait SimpleOperationStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Клик по кнопке добавления тега.
      case AddBtnClick(event) =>
        _addBtnClicked()
    }

    /** Реакция на клик по кнопке добавления тега. */
    protected def _addBtnClicked(): Unit = {
      ???
    }

  }

}
