package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.m.mfoc.FocRootDisappeared
import io.suggest.sc.sjs.vm.foc.FRoot
import io.suggest.sc.ScConstants.Focused.SLIDE_ANIMATE_MS
import io.suggest.sc.sjs.vm.res.FocusedRes
import org.scalajs.dom

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.15 16:04
 * Description: Аддон с кодом для сборки состояния закрытия focused-выдачи.
 */
trait Closing extends MouseMoving {

  /** Трейт состояния закрытия focused-выдачи. */
  protected trait FocClosingStateT extends FocMouseMovingStateT {

    override def afterBecome(): Unit = {
      super.afterBecome()
      for (froot <- FRoot.find()) {
        froot.willAnimate()
        dom.setTimeout(
          {() =>
            froot.disappearTransition()
            dom.setTimeout(
              { () => _sendEvent(FocRootDisappeared) },
              SLIDE_ANIMATE_MS
            )
          },
          5
        )
      }
    }

    override def receiverPart: Receive = super.receiverPart orElse {
      case FocRootDisappeared =>
        _disappeared()
    }

    /** Логика реакции на наступления сокрытости focused-выдачи. */
    protected def _disappeared(): Unit = {
      // В фоне надо очистить focused-верстку от мусора и освежить базовый каркас focused-выдачи.
      dom.setTimeout(
        {() =>
          for (froot <- FRoot.find()) {
            froot.wontAnimate()
            froot.clear()
            for (res <- FocusedRes.find()) {
              res.clear()
            }
          }
        },
        10
      )
      val sd1 = _stateData.copy(
        focused = None
      )
      become(_afterDisappearState, sd1)
    }

    /** Состояние, на которое надо переключиться после окончания сокрытия focused-выдачи. */
    protected def _afterDisappearState: FsmState

  }

}
