package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.m.mfoc._
import io.suggest.sc.sjs.m.msrv.foc.find.IFocAd
import io.suggest.sc.sjs.vm.foc.{FCarousel, FRoot}
import io.suggest.sc.sjs.vm.foc.fad.FAdRoot
import org.scalajs.dom
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.ext.KeyCode

import scala.collection.immutable.Queue

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.08.15 18:44
 * Description: Аддон для сборки состояний нахождения "в фокусе", т.е. на УЖЕ открытой focused-карточки.
 */
trait OnFocus extends ScFsmStub {

  /** Заготовка для состояний, связанных с нахождением на карточке.
    * Тут реакция на события воздействия пользователя на focused-выдачу. */
  protected trait OnFocusStateBaseT extends FsmState with INodeSwitchState {

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
        fRoot     <- FRoot.find()
        fState    <- {
          fRoot.willAnimate()
          sd0.focused
        }
        currIndex <- fState.currIndex
        _fad      <- fState.carState
          .find { _fad => _fad.index == currIndex }
      } {
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

  /** Состояние нахождения в фокусе одной карточки.
    * Помимо обработки сигналов это состояние готовит соседние карточки к отображению. */
  protected trait OnFocusStateT extends OnFocusStateBaseT {

    override def afterBecome(): Unit = {
      super.afterBecome()
      val sd0 = _stateData
      for {
        fState      <- sd0.focused
        currIndex   <- fState.currIndex
        screen      <- sd0.screen
        car         <- FCarousel.find()
        totalCount  <- fState.totalCount
      } {
        // Сначала идёт дедубликация кода повторяющейся логики обработки prev и next карточек.

        // Код проверки необходимости инжекции в карусель offscreen-карточки. Pure function.
        def __isMissingInCar(index: Int, carState: CarState): Boolean = {
          // Если текущая карточка -- крайняя в focused-выборке?
          val isCurrentLast = totalCount == index
          // Да, если текущая карточка не крайняя и в этом краю carState нет элемента с nextIndex.
          !isCurrentLast && !carState.headOption.exists(_.index == index)
        }

        // Код опциональной инжекции карточек в карусель в зависимости от флага. Функция даёт side-effect'ы на DOM.
        def __maybeFadInject(isNeeded: Boolean, q0: FAdQueue, carState0: CarState, index: Int)
                            (pushCellF: FAdRoot => Unit): (FAdQueue, CarState) = {
          if (isNeeded) {
            // Залить в карусель недостающую next-карточку, которая уже присутствует в nexts-аккамуляторе состояния.
            val (_nextFad, _nexts3) = q0.dequeue
            // Приаттачить карточку в DOM.
            val fadRoot = FAdRoot( _nextFad.bodyHtml )
            fadRoot.initLayout( screen )
            fadRoot.setLeftPx( index * screen.width )
            pushCellF(fadRoot)
            //car.pushCellRight( fadRoot )
            // Подготовить обновлённые данные состояния FSM.
            val fadShown = FAdShown(fadRoot, _nextFad)
            (_nexts3, fadShown :: carState0)
          } else {
            // Заливка в карусель не требуется, вернуть текущие данные.
            (q0, carState0)
          }
        }
        // Кода сборки финального состояния FSM. Pure function.
        def __makeSd(prevs: FAdQueue, carState: CarState, nexts: FAdQueue): SD = {
          sd0.copy(
            focused = Some(fState.copy(
              prevs     = prevs,
              carState  = carState,
              nexts     = nexts
            ))
          )
        }

        // Извлечь из карусели карточки, находящиеся дальше от текущей чем 1 хоп.
        val prevIndex = Math.max(0, currIndex - 1)
        val nextIndex = Math.min(currIndex + 1, totalCount - 1)
        val (prevs2, carState2Rev, nexts2) = {
          fState.carState.foldLeft((fState.prevs, List.empty[FAdShown], fState.nexts)) {
            case ((prevs, retain, nexts), e) =>
              if (e.index < prevIndex)
                (e.mFocAdImpl +: prevs, retain, nexts)
              else if (e.index <= nextIndex)
                (prevs, e :: retain, nexts)
              else
                (prevs, retain, e.mFocAdImpl +: nexts)
          }
        }

        // TODO Приаттачить ближайшие карточки в карусель (prev, next) из состояния, если есть.
        // Отсутствует ли в карусели next элемент?...
        val nextMissingInCar = __isMissingInCar(nextIndex, carState2Rev)

        // Если не хватает next-карточки в карусели, то попытаться найти её в nexts.
        if (nextMissingInCar && fState.nexts.isEmpty) {
          // Нужен next-элемент, которого нет. Надо перейти на состяние опережающей подгрузки next-карточек.
          val sd1 = __makeSd(prevs2, carState2Rev.reverse, nexts2)
          become(_nextPreLoadState, sd1)

        } else {
          // Если требуется перенести next-карточку в карусель, то сделать это.
          val (nexts3, carState3Rev) = __maybeFadInject(nextMissingInCar, fState.nexts, carState2Rev, nextIndex)(car.pushCellRight)

          // === next-карточка отработана ===
          // Теперь надо отработать prev-карточку аналогично.

          val carState3 = carState3Rev.reverse

          val prevMissingInCar = __isMissingInCar(prevIndex, carState3)
          if (prevMissingInCar && fState.prevs.isEmpty) {
            // Нужен prev-элемент, которого нет. Надо перейти на состояние опережающей подгрузки prev-карточек.
            val sd1 = __makeSd(prevs2, carState3, nexts3)
            become(_prevPreLoadState, sd1)
          } else {
            // Если требуется перенести prev-карточку в карусель, то сделать это.
            val (prevs4, carState4) = __maybeFadInject(prevMissingInCar, fState.prevs, carState3, prevIndex)(car.pushCellLeft)

            // prev-карточка отработана. Тут больше ничего делать не надо. Залить новые данные в состояние.
            _stateData = __makeSd(prevs4, carState4, prevs4)
          } // prevMissingInCar else

        }   // nextMissingInCar else

      }     // for()
    }       // afterBecome()

  }         // trait


  protected def _nextPreLoadState: FsmState = ???
  protected def _prevPreLoadState: FsmState = ???

}
