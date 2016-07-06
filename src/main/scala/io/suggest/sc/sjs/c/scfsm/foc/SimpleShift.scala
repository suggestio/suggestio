package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.m.mfoc.{IFocAd, IFocSd, MFocCurrSd}
import io.suggest.sc.sjs.vm.foc.{FCarCont, FControls}
import io.suggest.sc.ScConstants.Focused.SLIDE_ANIMATE_MS
import io.suggest.sc.sjs.vm.foc.fad.FArrow
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

  protected trait IShiftDoneState {
    protected def _shiftDoneState: FsmState
  }

  /** Трейт для сборки состояния автоматического перехода на следующую/предыдущую карточку. */
  protected trait SimpleShiftStateT extends FocMouseMovingStateT with OnFocusDelayedResize with IShiftDoneState {

    /** Сообщение о завершении анимации переключения карточки. */
    protected case object ShiftAnimationFinished extends IFsmMsg

    /** Определить данные новой текущей карточки.
      *
      * @return None, когда переключение невозможно и требуется возврат на текущую карточку.
      */
    protected def _nextCurrent(fState: IFocSd): Option[IFocAd]
    protected def _nextIndexOffset: Int

    override def afterBecome(): Unit = {
      super.afterBecome()
      val sd0 = _stateData
      for {
        fState        <- sd0.focused
        car           <- FCarCont.find()
      } {
        _nextCurrent(fState).fold[Unit] {
          // Нет следующей карточки -- нет работы. Сворачиваемся.
          become(_shiftDoneState)

        } { nextFad =>
          val nextCurr = MFocCurrSd(
            madId = nextFad.madId,
            index = fState.current.index + _nextIndexOffset
          )

          // Есть индекс следующей карточки. Запустить анимацию карусели в нужном направлении.
          car.animateToCell(nextCurr.index, sd0.common)

          // Залить новый заголовок в выдачу и состояние, если продьюсер новой карточки отличается от текущего.
          if (!fState.findCurrFad.exists( _.producerId == nextFad.producerId )) {
            for (fControls <- FControls.find()) {
              // Перезаписать заголовок, стрелочки и остальные focused-контролы.
              fControls.setContent(nextFad.controlsHtml)
              // Выставить снова направление стрелочки под курсором мыши.
              for {
                curDir  <- fState.arrDir
                fArr    <- FArrow.find()
              } {
                fArr.setDirection(curDir)
              }
            }
          }

          DomQuick.setTimeout(SLIDE_ANIMATE_MS) {() =>
            _sendEvent(ShiftAnimationFinished)
          }

          // Залить новый текущий индекс в состояние.
          _stateData = sd0.copy(
            focused = Some(fState.copy(
              current = nextCurr
            ))
          )
        }
      }
    }

    override def receiverPart: Receive = {
      case ShiftAnimationFinished =>
        _animationFinished()
    }

    /** Логика реакции на завершение анимации. */
    protected def _animationFinished(): Unit = {
      become(_shiftDoneState)
    }
  }
  
  
  /** Трейт для сборки состояния перехода на одну карточку влево. */
  protected trait ShiftLeftStateT extends SimpleShiftStateT {
    /** Определить предыдущую карточку. */
    override def _nextCurrent(fState: IFocSd): Option[IFocAd] = {
      fState.fadsBeforeCurrentIter
        .toSeq
        .lastOption
    }

    override def _nextIndexOffset = -1
  }

  /** Трейт для сборки состояния перехода на одну карточку вправо. */
  protected trait ShiftRightStateT extends SimpleShiftStateT {
    override def _nextCurrent(fState: IFocSd): Option[IFocAd] = {
      fState.fadsAfterCurrentIter
        .toStream
        .headOption
    }
    override def _nextIndexOffset = +1
  }

}
