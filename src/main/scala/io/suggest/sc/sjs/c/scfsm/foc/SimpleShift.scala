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

          // Залить новый заголовок в выдачу и состояние.
          for {
            fadShown  <- fState.shownFadWithIndex(nextIndex)
            fControls <- FControls.find()
          } yield {
            val oldHtml = fControls.innerHtml
            // Если producer тот же, то заголовок трогать не надо.
            val needUpdateControls = !fState.shownFadWithIndex(currIndex)
              .exists(_.producerId == fadShown.producerId)
            fState.carState.map { fadShown1 =>
              val i = fadShown.index
              val ch1 = if (i == currIndex) {
                Left(oldHtml)
              } else if (i == nextIndex) {
                if (needUpdateControls)
                  fControls.setContent( fadShown.controlsHtml )
                Right(fControls)
              } else {
                null
              }
              if (ch1 != null) {
                fadShown1.copy(
                  controlsHtmlOpt = ch1
                )
              } else {
                fadShown1
              }
            }
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

    private def _receiverPart: Receive = {
      case ShiftAnimationFinished =>
        _animationFinished()
    }

    override def receiverPart: Receive = _receiverPart orElse super.receiverPart

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
