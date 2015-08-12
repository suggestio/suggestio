package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.mgrid.{GridBlockClick, GridScroll}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 10:13
 * Description: Аддон для поддержки сборки состояний выдачи, связанных с возможностями плитки карточек.
 */
trait OnGrid extends ScFsmStub {

  /** Трейт для сборки состояний, в которых доступна сетка карточек, клики по ней и скроллинг. */
  protected trait OnGridStateT extends FsmState {

    /** Обработка кликов по карточкам в сетке. */
    protected def handleGridBlockClick(gbc: GridBlockClick): Unit = {
      ???
    }

    /** Реакция на вертикальный скроллинг. */
    protected def handleVScroll(vs: GridScroll): Unit = {
      ???
    }

    private def _receiverPart: Receive = {
      // Клик по одной из карточек в сетке оных.
      case gbc: GridBlockClick =>
        handleGridBlockClick(gbc)

      // Вертикальный скроллинг в плитке.
      case vs: GridScroll =>
        handleVScroll(vs)
    }

    /** Обработка событий сетки карточек. */
    override def receiverPart: Receive = {
      _receiverPart orElse super.receiverPart
    }

  }

}



