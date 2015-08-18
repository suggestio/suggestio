package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.m.mfoc.{ProducerLogoClick, CloseBtnClick}
import io.suggest.sc.sjs.vm.foc.FRoot
import org.scalajs.dom
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.ext.KeyCode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.08.15 18:44
 * Description: Аддон для сборки состояний нахождения "в фокусе", т.е. на УЖЕ открытой focused-карточки.
 */
trait OnFocus extends ScFsmStub {

  /** Состояние нахождения в фокусе одной карточки.
    * Помимо обработки сигналов это состояние готовит соседние карточки к отображению. */
  protected trait OnFocusState extends FsmState with INodeSwitchState {

    override def afterBecome(): Unit = {
      super.afterBecome()
      // TODO Подгрузить/подготовить к отображению соседние карточки, если необходимо.
    }

    private def _receiverPart: Receive = {
      case CloseBtnClick =>
        _closeFocused()
      case ProducerLogoClick =>
        _goToProducer()
      // TODO Реакция на next/prev click и touch-события
    }

    override def receiverPart: Receive = _receiverPart orElse super.receiverPart

    override protected def _onKbdKeyUp(event: KeyboardEvent): Unit = {
      super._onKbdKeyUp(event)
      // ESC должен закрывать выдачу.
      if (event.keyCode == KeyCode.escape) {
        _closeFocused()
      }
      // TODO Реакция на нажатие стрелок на клаве left/right (next/prev).
    }

    /** Реакция на клик по кнопке закрытия или иному выходу из focused-выдачи. */
    protected def _closeFocused(): Unit = {
      become(_closingState)
    }

    /** Состояние процесса закрытия focused-выдачи. */
    protected def _closingState: FsmState

    /** Реакция на сигнал перехода на producer-выдачу. */
    protected def _goToProducer(): Unit = {
      val sd0 = _stateData
      // Найти в состоянии текущую карточку. Узнать продьюсера,
      for {
        fState    <- sd0.focused
        currIndex <- fState.currIndex
        _fad      <- fState.carState
          .find { _fad => _fad.index == currIndex }
        fRoot     <- FRoot.find()
      } {
        fRoot.willAnimate()
        val adnId = _fad.producerId
        if (sd0.adnIdOpt contains adnId) {
          // Возврат на плитку текуйщего узла отрабатывается соответствующим состоянием.
          become(_closingState)

        } else {
          // Переход на другой узел.
          val sd1 = sd0.withNodeSwitch( Some(adnId) )
          become(_onNodeSwitchState, sd1)
          // Скрыть текущую focused-выдачу в фоне. Глубокая чистка не требуется, т.к. layout будет полностью пересоздан.
          dom.setTimeout(
            { () => fRoot.disappearTransition() },
            10
          )
        }
      }
    }
    
  }
  // TODO Приаттачить оставшиеся карточки в карусель (prev, next) из состояния. Обновить состояние.

}
