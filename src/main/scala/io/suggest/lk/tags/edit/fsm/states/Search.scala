package io.suggest.lk.tags.edit.fsm.states

import io.suggest.lk.tags.edit.fsm.TagsEditFsmStub
import io.suggest.lk.tags.edit.m.signals.NameInputEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 14:27
 * Description: Аддон для сборки состояний, связанных с текстовым поиском тегов.
 */
trait Search extends TagsEditFsmStub {

  /** Аддон для состояния для запуска поискового запроса. */
  protected trait SearchStartStateT extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить поисковый запрос, повесить на него листенер.
      ???
    }
  }


  /** Трейт состояния с ожиданием ответа по поисковому запросу. */
  protected trait SearchWaitStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Продолжается набор текста, хотя запрос поиска уже запущен.
      case NameInputEvent(event) =>
        ???
    }
  }

}
