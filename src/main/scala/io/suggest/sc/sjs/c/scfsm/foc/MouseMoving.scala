package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mfoc.MFocSd
import io.suggest.sc.sjs.vm.foc.fad.FArrow
import io.suggest.sjs.common.model.{MHands, MHand}
import org.scalajs.dom.MouseEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.15 15:40
 * Description: Трейт для сборки аддонов состояния, поддерживающих реакцию на Mouse move.
 */
trait MouseMoving extends ScFsmStub {

  /** Аддон для сборки focused-состояний, поддерживающих событие MouseMove.
    * Так же тут есть кое-какое API для мышиных нужд. */
  protected trait FocMouseMovingStateT extends FsmState {

    /** Логика обработки сигнала о движении мышки. */
    override def onMouseMove(event: MouseEvent): Unit = {
      super.onMouseMove(event)
      val sd0 = _stateData
      for {
        screen  <- sd0.common.screen
        fState  <- sd0.focused
        fArr    <- FArrow.find()
      } {
        // Обновить направление стрелки и состояние FSM, если требуется.
        val mhand = _mouse2hand(event, screen)
        _maybeUpdateArrDir(mhand, fArr, fState, sd0)

        // Обновить координаты стрелочки.
        fArr.updateX(event.clientX.toInt)
        fArr.updateY(event.clientY.toInt)
      }
    }

    /** Находится ли курсор мыши в левой части экрана? */
    protected def _isMouseRight(event: MouseEvent, screen: IMScreen): Boolean = {
      event.clientX > screen.width / 2
    }

    protected def _mouse2hand(event: MouseEvent, screen: IMScreen): MHand = {
      if (_isMouseRight(event, screen))
        MHands.Right
      else
        MHands.Left
    }

    protected def _maybeUpdateArrDir(mhand: MHand, fArr: FArrow, fState: MFocSd, sd0: SD = _stateData): Unit = {
      if (!(fState.arrDir contains mhand)) {
        fArr.setDirection(mhand)
        // Сохранить новый direction в состояние.
        _stateData = sd0.copy(
          focused = Some(fState.copy(
            arrDir = Some(mhand)
          ))
        )
      }
    }

  }

}
