package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.m.mfoc.IFocSd
import io.suggest.sc.sjs.vm.foc.{FCarousel, FControls}
import io.suggest.sc.ScConstants.Focused.SLIDE_ANIMATE_MS
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.fsm.IFsmMsg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.08.15 17:11
 * Description: Аддон для ScFsm с трейтами для сборки состояний автоперехода между карточками.
 * Эта логика не подходит для touch-переключений -- там совсем другая логика работы.
 */
trait SimpleShift extends MouseMoving {

  /** Трейт для сборки состояния автоматического перехода на следующую/предыдущую карточку. */
  protected trait SimpleShiftStateT extends FocMouseMovingStateT {

    /** Сообщение о завершении анимации переключения карточки. */
    protected case object ShiftAnimationFinished extends IFsmMsg

    protected def _nextIndex(currIndex: Int, fState: IFocSd): Int

    override def afterBecome(): Unit = {
      super.afterBecome()
      val sd0 = _stateData
      for {
        fState        <- sd0.focused
        car           <- FCarousel.find()
        currIndex     <- fState.currIndex
        screen        <- sd0.screen
      } {
        val nextIndex = _nextIndex(currIndex, fState)
        if (nextIndex == currIndex) {
          // Нет следующей карточки -- нет работы. Сворачиваемся.
          become(_shiftDoneState)
        } else {
          // Есть индекс следующей карточки. Запустить анимацию карусели в нужном направлении.
          car.animateToCell(nextIndex, screen, sd0.browser)

          // Залить новый заголовок в выдачу и состояние, если продьюсер новой карточки отличается от текущего.
          for {
            nextFad <- fState.shownFadWithIndex(nextIndex)
            if !(fState.shownFadWithIndex(currIndex) exists {
              _.producerId == nextFad.producerId
            })
            fControls <- FControls.find()
          } {
            fControls.setContent(nextFad.controlsHtml)
          }

          DomQuick.setTimeout(SLIDE_ANIMATE_MS) {() =>
            _sendEvent(ShiftAnimationFinished)
          }

          // Залить новый текущий индекс в состояние.
          _stateData = sd0.copy(
            focused = Some(fState.copy(
              currIndex = Some(nextIndex)
            ))
          )
        }
      }
    }

    override def receiverPart: Receive = {
      case ShiftAnimationFinished =>
        _animationFinished()
    }

    protected def _shiftDoneState: FsmState

    /** Логика реакции на завершение анимации. */
    protected def _animationFinished(): Unit = {
      become(_shiftDoneState)
    }
  }
  
  
  /** Трейт для сборки состояния перехода на одну карточку влево. */
  protected trait ShiftLeftStateT extends SimpleShiftStateT {
    override protected def _nextIndex(currIndex: Int, fState: IFocSd): Int = {
      Math.max(0, currIndex - 1)
    }
  }

  /** Трейт для сборки состояния перехода на одну карточку вправо. */
  protected trait ShiftRightStateT extends SimpleShiftStateT {
    override protected def _nextIndex(currIndex: Int, fState: IFocSd): Int = {
      val nextIndex0 = currIndex + 1
      fState.totalCount.fold(nextIndex0) { tc =>
        Math.min(tc - 1, nextIndex0)
      }
    }
  }

}
