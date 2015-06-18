package io.suggest.sc.sjs.vm.foc.fsm

import io.suggest.sc.sjs.m.mfoc.FocVmStateData
import io.suggest.sc.sjs.vm.foc.FocAdVm
import io.suggest.sc.sjs.vm.foc.fsm.msg._
import io.suggest.sc.sjs.vm.fsm.ScFsmStub

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 11:19
 * Description: FSM-код для сборки конечного автомата, обслуживающего focused-подсистему.
 */
trait FocFsm extends ScFsmStub {

  /** Внутреннее состояние focused-выдачи. Или null, когда focused-выдача неактивна. */
  private var _data: FocVmStateData = null


  /** Трейт для состояния, когда focused-выдача отсутствует, скрыта вообще и ожидает активации.
    * При появлении top-level ScFsm это событие исчезнет, и будет обрабатываться где-то в вышестоящем обработчике. */
  protected trait UnfocusedStateT extends FsmState {

    /** Реакция на событие GoTo() перемотки на указанную по порядку карточку. */
    private def _handleFocGoTo(index: Int): Unit = {
      ???
    }

    override def receiverPart: Receive = {
      // Сигнал к открытию focused-выдачи на указанной карточке в tile-выдаче.
      // Нужно создать состояние и запустить фокусировку на индексе 0.
      case GoTo(index) =>
        _data = FocVmStateData()
        _handleFocGoTo(index)

      // Сигнал к расфокусировке, но focused-выдача и так закрыта. Молча игнорим.
      case Close =>
    }
  }


  /** Состояние, когда запрошены у сервера карточки. */
  protected class WaitForFadsState(nextIndex: Int) extends FsmState {
    override def receiverPart: Receive = {
      case FadsReceived(fads) =>
        val fadsIter2 = fads.focusedAdsIter.map { fad =>
          fad.index -> FocAdVm(fad)
        }
        val sd0 = _data
        val stateData1 = sd0.copy(
          ads           = sd0.ads ++ fadsIter2,
          loadedCount   = sd0.loadedCount + fads.fadsCount,
          totalCount    = Some(fads.totalCount)
        )
        // TODO Переключиться на следующее состояние.
        ???
    }
  }

}
