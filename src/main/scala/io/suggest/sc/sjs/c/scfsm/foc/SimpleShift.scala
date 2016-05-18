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
  *
  * Эта же логика используется и в touch-перелистывании: автоматическое завершение
  * перелистывания влево/вправо происходит с помощью этого же кода.
  */
trait SimpleShift extends MouseMoving with OnFocusBase {

  /** Трейт для сборки состояния автоматического перехода на следующую/предыдущую карточку. */
  protected trait SimpleShiftStateT extends FocMouseMovingStateT with OnFocusDelayedResize {

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
          car.animateToCell(nextIndex, screen, sd0.common.browser)

          // Залить новый заголовок в выдачу и состояние, если продьюсер новой карточки отличается от текущего.
          val nextFadOpt = fState.shownFadWithIndex(nextIndex)
          for {
            nextFad <- nextFadOpt
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
              currAdId  = nextFadOpt.map(_.madId),
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
