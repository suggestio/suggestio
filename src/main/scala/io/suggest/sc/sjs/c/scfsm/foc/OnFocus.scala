package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.m.mfoc._
import io.suggest.sc.sjs.m.mfsm.touch.TouchStart
import io.suggest.sc.sjs.m.mv.MTouchLock
import io.suggest.sc.sjs.vm.foc.{FCarousel, FRoot}
import io.suggest.sc.sjs.vm.foc.fad.{FAdWrapper, FArrow, FAdRoot}
import io.suggest.sjs.common.geom.Coord2dD
import io.suggest.sjs.common.model.{MHand, MHands}
import io.suggest.sc.ScConstants.Focused.FAd.KBD_SCROLL_STEP_PX
import io.suggest.sjs.common.util.TouchUtil
import org.scalajs.dom
import org.scalajs.dom.{TouchEvent, MouseEvent, KeyboardEvent}
import org.scalajs.dom.ext.KeyCode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.08.15 18:44
 * Description: Аддон для сборки состояний нахождения "в фокусе", т.е. на УЖЕ открытой focused-карточки.
 * Base -- трейт с вещами, расшаренными между трейтами конкретных состояний.
 */
trait OnFocusBase extends MouseMoving {

  protected trait ISimpleShift {
    /** Состояние переключения на следующую карточку. */
    protected def _shiftRightState: FsmState
    /** Состояние переключения на предыдущую карточку. */
    protected def _shiftLeftState : FsmState
  }


  /** Заготовка для состояний, связанных с нахождением на карточке.
    * Тут реакция на события воздействия пользователя на focused-выдачу. */
  protected trait OnFocusStateBaseT extends FsmEmptyReceiverState with FocMouseMovingStateT with INodeSwitchState with ISimpleShift {

    override def receiverPart: Receive = super.receiverPart orElse {
      case TouchStart(event) =>
        _onTouchStart(event)
      case CloseBtnClick =>
        _closeFocused()
      case ProducerLogoClick =>
        _goToProducer()
      case MouseClick(evt) =>
        _mouseClicked(evt)
    }

    /** С началом свайпа надо инициализировать touch-параметры и перейти в свайп-состояние. */
    protected def _onTouchStart(event: TouchEvent): Unit = {
      val sd0 = _stateData
      val touch = event.touches(0)
      val sd1 = sd0.copy(
        focused = sd0.focused.map( _.copy(
          touch = Some(MFocTouchSd(
            start = Coord2dD( touch ),
            lastX = touch.pageX
          ))
        ))
      )
      become(_onTouchStartState, sd1)
    }
    protected def _onTouchStartState: FsmState


    override def _onKbdKeyUp(event: KeyboardEvent): Unit = {
      super._onKbdKeyUp(event)
      val c = event.keyCode
      // ESC должен закрывать выдачу.
      if (c == KeyCode.Escape)
        _closeFocused()
      // Клавиатурные стрелки влево-вправо должны переключать карточки в соотв. направлениях.
      else if (c == KeyCode.Right)
        _kbdShifting( MHands.Right, _shiftRightState )
      else if (c == KeyCode.Left)
        _kbdShifting( MHands.Left, _shiftLeftState )
      // TODO Скроллинг должен быть непрерывным. Сейчас он срабатывает только при отжатии клавиатурных кнопок скролла.
      else if (c == KeyCode.Down)
        _kbdScroll( KBD_SCROLL_STEP_PX )
      else if (c == KeyCode.Up)
        _kbdScroll( -KBD_SCROLL_STEP_PX )
      else if (c == KeyCode.PageDown)
        _kbdScroll( screenH )
      else if (c == KeyCode.PageUp)
        _kbdScroll( -screenH )
    }

    private def screenH: Int = _stateData.screen.fold(480)(_.height)

    protected def _kbdScroll(delta: Int): Unit = {
      // Найти враппер текущей карточки и проскроллить его немного вниз.
      for {
        fState    <- _stateData.focused
        currIndex <- fState.currIndex
        fWrap     <- FAdWrapper.find(currIndex)
      } {
        fWrap.vScrollByPx(delta)
      }
    }

    /** Реакция на переключение focused-карточек стрелками клавиатуры.
      * @param dir направление переключения.
      */
    protected def _kbdShifting(dir: MHand, nextState: FsmState): Unit = {
      if (_filterSimpleShiftSignal(dir)) {
        val sd0 = _stateData
        val sd1 = sd0.copy(
          focused = sd0.focused.map(_.copy(
            arrDir = Some(dir)
          ))
        )
        become(nextState, sd1)
      }
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

    /** Можно игнорить шифтинг карточек в указанном направлении с помощью этого флага.
      * TODO Это костыль. По хорошему надо шифтить на следующую карточку и отображать там loader, а потом это сразу перезаписывать.
      */
    protected def _filterSimpleShiftSignal(mhand: MHand): Boolean = {
      true
    }

    /** Реакция на кликанье мышки по focused-выдаче. Надо понять, слева или справа был клик, затем
      * запустить листание в нужную сторону. */
    protected def _mouseClicked(event: MouseEvent): Unit = {
      val sd0 = _stateData
      println( "touch: lock=" + MTouchLock() + " isTouchDev=" + TouchUtil.IS_TOUCH_DEVICE )
      for (screen <- sd0.screen;  fState <- sd0.focused) {
        val mhand = _mouse2hand(event, screen)
        for (fArr <- FArrow.find()) {
          _maybeUpdateArrDir(mhand, fArr, fState, sd0)
        }
        if (_filterSimpleShiftSignal(mhand)) {
          val nextState = if (mhand.isLeft) {
            _shiftLeftState
          } else {
            _shiftRightState
          }
          become(nextState)
        }
      }
    }

  }

}


/** Аддон для [[io.suggest.sc.sjs.c.ScFsm]] с трейт-реализацией состояния спокойного нахождения в focused-выдаче. */
trait OnFocus extends OnFocusBase {

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
            fadRoot.initLayout( screen, sd0.browser )
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

        // Код сборки финального состояния FSM. Pure function.
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
                (e.unshow() +: prevs, retain, nexts)
              else if (e.index <= nextIndex)
                (prevs, e :: retain, nexts)
              else
                (prevs, retain, e.unshow() +: nexts)
          }
        }

        // TODO Приаттачить ближайшие карточки в карусель (prev, next) из состояния, если есть.
        // Отсутствует ли в карусели next элемент?...
        val nextMissingInCar = __isMissingInCar(nextIndex, carState2Rev)

        // Если не хватает next-карточки в карусели, то попытаться найти её в nexts.
        if (nextMissingInCar && nexts2.isEmpty) {
          // Нужен next-элемент, которого нет. Надо перейти на состяние опережающей подгрузки next-карточек.
          val sd1 = __makeSd(prevs2, carState2Rev.reverse, nexts2)
          become(_rightPreLoadState, sd1)

        } else {
          // Поправить текущую ширину карусели, чтобы стало тык-впритык.
          car.setCellWidth(nextIndex, screen)

          // Если требуется перенести next-карточку в карусель, то сделать это.
          val (nexts3, carState3Rev) = __maybeFadInject(nextMissingInCar, nexts2, carState2Rev, nextIndex)(car.pushCellRight)

          // === next-карточка отработана ===
          // Теперь надо отработать prev-карточку аналогично.

          val carState3 = carState3Rev.reverse

          val prevMissingInCar = __isMissingInCar(prevIndex, carState3)
          if (prevMissingInCar && prevs2.isEmpty) {
            // Нужен prev-элемент, которого нет. Надо перейти на состояние опережающей подгрузки prev-карточек.
            val sd1 = __makeSd(prevs2, carState3, nexts3)
            become(_leftPreLoadState, sd1)
          } else {
            // Если требуется перенести prev-карточку в карусель, то сделать это.
            val (prevs4, carState4) = __maybeFadInject(prevMissingInCar, prevs2, carState3, prevIndex)(car.pushCellLeft)

            // prev-карточка отработана. Тут больше ничего делать не надо. Залить новые данные в состояние.
            _stateData = __makeSd(prevs4, carState4, nexts3)
          } // prevMissingInCar else

        }   // nextMissingInCar else

      }     // for()
    }       // afterBecome()


    /** Состояние OnFocus с подгрузкой предшествующих карточек (слева). */
    protected def _leftPreLoadState: FsmState

    /** Состояние OnFocus с подгрузкой последующих карточек (справа). */
    protected def _rightPreLoadState: FsmState

  }         // FSM state trait

}
