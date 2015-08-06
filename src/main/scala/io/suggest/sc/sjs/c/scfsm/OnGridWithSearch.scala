package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.mhdr.HideSearchClick
import io.suggest.sc.sjs.vm.hdr.HRoot
import io.suggest.sc.sjs.vm.search.SRoot
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 13:52
 * Description: FSM-Аддон для добавления поддержки состояния выдачи, когда доступна плитка и открыта панель поиска.
 */
trait OnGridWithSearch extends OnGrid {

  protected trait OnGridWithSearchStateT extends OnGridStateT with PanelGridRebuilder {
    
    protected def _hideSearchClick(evt: Event): Unit = {
      val sd0 = _stateData
      for (sroot <- SRoot.find(); screen <- sd0.screen) {
        // Показать панель
        sroot.hide()
        // Сменить набор кнопок в заголовке.
        for (header <- HRoot.find()) {
          header.hideBackToIndexBtns()
        }

        // Отребилдить плитку карточек, создав новое состояние выдачи.
        val sd1 = _rebuildGridOnPanelChange(sd0, screen, sroot)

        // Сменить состояние на то, где открыта панель поиска.
        become(_nextStateSearchPanelClosed(sd1), sd1)
      }
    }


    protected def _nextStateSearchPanelClosed(sd1: SD): FsmState

    override def receiverPart: PartialFunction[Any, Unit] = super.receiverPart orElse {
      case HideSearchClick(evt) =>
        _hideSearchClick(evt)
    }

  }

}
