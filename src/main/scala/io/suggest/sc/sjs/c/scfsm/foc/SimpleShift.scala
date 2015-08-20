package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.m.mfoc.IFocSd
import io.suggest.sc.sjs.m.mfsm.IFsmMsg
import io.suggest.sc.sjs.vm.foc.{FControls, FCarousel}
import io.suggest.sc.ScConstants.Focused.SLIDE_ANIMATE_MS
import org.scalajs.dom

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.08.15 17:11
 * Description: Аддон для ScFsm с трейтами для сборки состояний автоперехода между карточками.
 * Эта логика не подходит для touch-переключений -- там совсем другая логика работы.
 */
trait SimpleShift extends ScFsmStub {

  /** Трейт для сборки состояния автоматического перехода на следующую/предыдущую карточку. */
  protected trait SimpleShiftStateT extends FsmState {

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
          car.animateToCell(nextIndex, screen)

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

          dom.setTimeout(
            {() => _sendEvent(ShiftAnimationFinished) },
            SLIDE_ANIMATE_MS
          )
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
      fState.totalCount.fold(nextIndex0)( Math.min(_, nextIndex0) )
    }
  }

}
