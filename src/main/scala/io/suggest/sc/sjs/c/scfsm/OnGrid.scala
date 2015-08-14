package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.mfoc.MFocSd
import io.suggest.sc.sjs.m.mgrid.{IGridBlockClick, GridBlockClick, GridScroll}

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


    /** Реакция на вертикальный скроллинг. */
    protected def handleVScroll(vs: GridScroll): Unit = {
      ???
    }

    private def _receiverPart: Receive = {
      // Клик по одной из карточек в сетке оных.
      case gbc: GridBlockClick =>
        handleGridBlockClick( gbc )

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



