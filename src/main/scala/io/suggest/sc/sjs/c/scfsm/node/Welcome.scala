package io.suggest.sc.sjs.c.scfsm.node

import io.suggest.sc.ScConstants.Welcome
import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.m.mwc.{IWcStepSignal, WcTimeout}
import io.suggest.sc.sjs.vm.wc.WcRoot
import org.scalajs.dom

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.15 15:41
 * Description: Аддон для ScFsm для сборки состояний, связанных с карточкой приветствия.
 */
trait Welcome extends ScFsmStub {

  /** Дедублицированный код опциональной отмены таймера, сохраненного в состоянии,
    * в зависимости от полученного сигнала. */
  private def _maybeCancelWcTimer(signal: IWcStepSignal): Unit = {
    if (signal.isUser)
      _stateData.maybeCancelTimer()
  }

  /** Интерфейс с методом, возвращающим выходное состояние (выход из welcome-фазы). */
  trait IWelcomeFinished {
    /** Состояние, когда welcome-карточка сокрыта и стёрта из DOM. */
    protected def _welcomeFinishedState: FsmState
  }

  trait IWelcomeHiding {
    /** Состояние, когда происходит плавное сокрытие welcome-карточки. */
    protected def _welcomeHidingState: FsmState
  }


  /** Трейт для сборки состояний нахождения на welcome-карточке. */
  trait OnWelcomeShownStateT extends FsmEmptyReceiverState with IWelcomeFinished with IWelcomeHiding {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Приём сигнала от таймера о необходимости начать сокрытие карточки приветствия. Либо юзер тыкает по welcome-карточке.
      case signal: IWcStepSignal =>
        _maybeCancelWcTimer(signal)
        _letsHideWelcome()
    }

    /** Необходимо запустить плавное сокрытие welcome-карточки. */
    protected def _letsHideWelcome(): Unit = {
      val wcRootOpt = WcRoot.find()
      val hideTimerIdOpt = for (wcRoot <- wcRootOpt) yield {
        wcRoot.fadeOut()
        dom.setTimeout(
          { () => _sendEventSyncSafe(WcTimeout) },
          Welcome.FADEOUT_TRANSITION_MS
        )
      }
      val nextState = wcRootOpt.fold[FsmState] (_welcomeFinishedState) (_ => _welcomeHidingState)
      val sd2 = _stateData.copy(
        timerId = hideTimerIdOpt
      )
      become(nextState, sd2)
    }

  }


  /** Трейт для сборки состояний, реагирующий на завершение плавного сокрытия welcome-карточки. */
  trait OnWelcomeHidingState extends FsmEmptyReceiverState with IWelcomeFinished {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Сработал таймер окончания анимированного сокрытия welcome. Или юзер тыкнул по welcome-карточке.
      case signal: IWcStepSignal =>
        _maybeCancelWcTimer(signal)
        _letsFinishWelcome()
    }

    protected def _letsFinishWelcome(): Unit = {
      for (wcRoot <- WcRoot.find()) {
        wcRoot.remove()
      }
      val sd2 = _stateData.copy(
        timerId = None
      )
      become(_welcomeFinishedState, sd2)
    }

  }


}
