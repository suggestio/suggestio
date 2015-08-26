package io.suggest.sc.sjs.c.scfsm.grid

import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.m.mfoc.MFocSd
import io.suggest.sc.sjs.m.mgrid.{GridBlockClick, GridScroll, IGridBlockClick}


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 10:13
 * Description: Аддон для поддержки сборки состояний выдачи, связанных с возможностями плитки карточек.
 */
trait OnGrid extends ScFsmStub {

  trait GridBlockClickStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Клик по одной из карточек в сетке оных.
      case gbc: GridBlockClick =>
        handleGridBlockClick( gbc )
    }

    /** Обработка кликов по карточкам в сетке. */
    protected def handleGridBlockClick(gbc: IGridBlockClick): Unit = {
      val gblock = gbc.gblock
      val sd0 = _stateData
      // Минимально инициализируем focused-состояние и переключаем логику на focused.
      val sd1 = sd0.copy(
        focused = Some(MFocSd(
          gblock = Some(gblock)
        ))
      )
      become(_startFocusOnAdState, sd1)
    }

    protected def _startFocusOnAdState: FsmState

  }


  /** Трейт для сборки состояний, в которых доступна сетка карточек, клики по ней и скроллинг. */
  trait OnGridStateT extends GridBlockClickStateT {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Вертикальный скроллинг в плитке.
      case vs: GridScroll =>
        handleVScroll(vs)
    }

    /** Реакция на вертикальный скроллинг. Нужно запросить с сервера ещё карточек. */
    protected def handleVScroll(vs: GridScroll): Unit = {
      if (!_stateData.grid.state.fullyLoaded) {
        become(_loadModeState)
      }
    }

    protected def _loadModeState: FsmState

  }

}



