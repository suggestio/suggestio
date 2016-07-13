package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.m.mfoc.MFocTouchSd
import io.suggest.sc.sjs.m.mfsm.touch.{TouchEnd, ITouchFinish}
import io.suggest.sc.sjs.vm.foc.FCarCont
import io.suggest.sjs.common.geom.Coord2dD
import io.suggest.sjs.common.model.MHands
import org.scalajs.dom.TouchEvent
import io.suggest.common.geom.coord.CoordOps._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.15 18:50
 * Description: Аддон для ScFsm для поддержки touch-навигации в focused-выдаче.
 */
trait OnTouch extends OnFocusBase {

  /** Интерфейс для метода, возвращающего экземпляр OnTouch-состояния. */
  protected trait FocTouchCancelledT extends OnFocusDelayedResize with OnFocSrvResp {

    /** Новое состояние после резкого завершения касания. */
    protected def _touchCancelledState: FsmState
    
    override def receiverPart: Receive = super.receiverPart.orElse {
      case t: ITouchFinish =>
        _touchCancelled()
    }

    protected def _clearTouchSd(sd0: SD = _stateData): SD = {
      sd0.copy(
        focused = sd0.focused.map(_.copy(
          touch = None
        ))
      )
    }

    /** Безрезультатное завершение касания -- сброс touch-состояния, переход на стабильное состояние. */
    protected def _touchCancelled(): Unit = {
      for (car <- FCarCont.find()) {
        car.enableTransition()
      }
      become(_touchCancelledState, _clearTouchSd())
    }

  }


  /** Трейт реализации состояния начала touch-навигации, когда нет текущего фонового прелоада
    * и ещё не ясно, что задумал юзер (верт.скролл или гориз.листание). */
  protected trait FocOnTouchStartStateT extends FocTouchCancelledT {

    override def afterBecome(): Unit = {
      super.afterBecome()
      // Нужно отключить transition у карусели, чтобы она не играла в догонялки с пальцем на экране.
      for (car <- FCarCont.find()) {
        car.disableTransition()
      }
    }

    /** Получение прямых сигналов TouchMove. */
    override def onTouchMove(event: TouchEvent): Unit = {
      super.onTouchMove(event)
      // Надо понять, в какую сторону двигаемся, и переключиться на соотв.состояние.
      val sd0 = _stateData
      val touch = event.touches(0)
      for {
        fState    <- sd0.focused
        touchSd   <- fState.touch
      } {
        val coord2 = Coord2dD(touch)

        val dx = touchSd.start.deltaX( coord2 )
        val dy = touchSd.start.deltaY( coord2 )

        if ( !(dx == 0d && dy == 0d) ) {
          val (nextState, sd1): (FsmState, SD) = {
            if ( Math.abs(dy) > Math.abs(dx) ) {
              (_touchVScrollState, sd0)
            } else {
              // Запрещаем вертикальный скроллинг для текущего события.
              event.preventDefault()
              // Для шифтинга надобно выставить lastX в состояние.
              val _sd1 = sd0.copy(
                focused = Some(fState.copy(
                  touch = Some(touchSd.copy(
                    lastX = coord2.x
                  ))
                ))
              )
              (_touchShiftState, _sd1)
            }
          }
          become(nextState, sd1)
        }
      }
    }

    /** Состояние вертикального скроллинга. */
    protected def _touchVScrollState: FsmState

    /** Состояние горизонтального переключения карточек. */
    protected def _touchShiftState: FsmState

  }
  
  
  /** Состояние вертикального скроллинга через touch.
    * Тут по сути делать ничего не надо кроме отработки окончания касания. */
  protected trait FocOnTouchScrollStateT extends FocTouchCancelledT


  /** Состояние горизонтальной навигации по focused-выдаче пальцем. */
  protected trait FocOnTouchShiftStateT extends FocTouchCancelledT with ISimpleShift {

    /** Одна и таже обработка нужна в нескольких местах.
      * Этот трейт нужен для дедубликации логики обработки мацанья пальцем по карусели. */
    protected trait MoveHelper {
      def _getX0(touchSd: MFocTouchSd): Double = {
        touchSd.start.x
      }
      def _getX1(touchSd: MFocTouchSd): Double
      def _touchSdUpdate(touchSd0: MFocTouchSd, lastDeltaX: Double, lastX: Double): MFocTouchSd

      def execute(): Unit = {
        val sd0 = _stateData
        for {
          fState     <- sd0.focused
          touchSd    <- fState.touch
          car        <- FCarCont.find()
        } {
          // Текущая координата исходного положения карусели в пикселях.
          val carX = FCarCont.indexToLeftPx(fState.current.index, sd0.common.screen)

          // В lastX из Start-состояния передаётся координата, пригодная для рассчета начальной deltaX.
          val lastX = _getX1(touchSd)
          val deltaX = _getX0(touchSd) - lastX

          // Нужно подвинуть выдачу согласно deltaX
          val carX2 = carX - deltaX.toInt
          car.animateToX(carX2, sd0.common.browser)

          // Залить в состояние данные по текущему направлению свайпа.
          _stateData = sd0.copy(
            focused = Some(fState.copy(
              touch = Some(
                _touchSdUpdate(touchSd, deltaX, lastX)
              )
            ))
          )
        }
      }
    }

    override def afterBecome(): Unit = {
      super.afterBecome()
      val h = new MoveHelper {
        override def _getX1(touchSd: MFocTouchSd): Double = {
          touchSd.lastX
        }
        override def _touchSdUpdate(touchSd0: MFocTouchSd, lastDeltaX: Double, lastX: Double): MFocTouchSd = {
          touchSd0.copy(
            lastDeltaX = Some( lastDeltaX )
          )
        }
      }
      h.execute()
    }


    override def receiverPart: Receive = {
      val rp: Receive = {
        case TouchEnd(event) =>
          _onTouchEnd(event)
      }
      rp.orElse( super.receiverPart )
    }

    /** Реакция на событие touchmove. */
    override def onTouchMove(event: TouchEvent): Unit = {
      super.onTouchMove(event)
      // Запрещаем вертикальный скроллинг.
      event.preventDefault()
      // Нужно горизонтально двигать карусель за пальцем, обновлять данные в состоянии.
      val h = new MoveHelper {
        override def _getX1(touchSd: MFocTouchSd): Double = {
          event.touches(0).pageX
        }
        override def _touchSdUpdate(touchSd0: MFocTouchSd, lastDeltaX: Double, lastX: Double): MFocTouchSd = {
          // Тут дополнительный if чтобы избежать случаев, когда в touchend не ясно, куда листали в итоге.
          val lastDeltaX2 = Some(lastDeltaX)
            .filter { ldx => Math.abs(lastDeltaX) < 0.2 }
            .orElse { touchSd0.lastDeltaX }
          touchSd0.copy(
            lastDeltaX = lastDeltaX2,
            lastX = lastX
          )
        }
      }
      h.execute()
    }

    /** Реакция на нормальное завершение касания.
      * Надо переключиться на состояние листания в итоговом направлении. */
    protected def _onTouchEnd(event: TouchEvent): Unit = {
      val sd0 = _stateData
      for {
        fState    <- sd0.focused
        touchSd   <- fState.touch
        car       <- FCarCont.find()
      } {
        // Подготовить focused-карусель к анимации.
        car.enableTransition()

        // Узнать следующее состояние FSM (долистывание в нужном направлении)
        val nextState = touchSd.lastDeltaX
          .filter { _ != 0d }
          // Нормализовать значение delta до "право" / "лево".
          .map { ldPx =>
            if (ldPx > 0d) MHands.Right else MHands.Left
          }
          // Отфильтровать свайп за экран
          .filter { mhand =>
            (mhand.isLeft && fState.fadsBeforeCurrentIter.nonEmpty) ||
              (mhand.isRight && fState.fadsAfterCurrentIter.nonEmpty)
          }
          .fold [FsmState] {
            // Сбросить сдвиг карусели на исходную.
            car.animateToCell(fState.current.index, sd0.common)
            _touchCancelledState
          }(_shiftForHand)

        // Выполнить переключение состояния FSM.
        val sd1 = _clearTouchSd(sd0)
        become(nextState, sd1)
      }
    }

  }

}
