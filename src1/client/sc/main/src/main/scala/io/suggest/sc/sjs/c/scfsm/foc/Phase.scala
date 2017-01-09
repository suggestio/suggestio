package io.suggest.sc.sjs.c.scfsm.foc

import io.suggest.sc.sjs.c.scfsm.ScFsm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.09.15 17:02
 * Description: Фаза состояний, описывающих нахождению юзера в focused-выдаче.
 */
trait Phase
  extends StartingForAd
  with OnFocus
  with Closing
  with SimpleShift
  with OnTouch
{
  that: ScFsm.type =>

  /** Реализация трейта поддержки ресайза. */
  protected trait IStartFocusOnAdState extends super.IStartFocusOnAdState {
    override def _startFocusOnAdState = new FocStartingForAd
  }

  /** Частичная реализация StartingForAdStateT. */
  protected trait StartingForAdStateT extends super.StartingForAdStateT with ProcessIndexReceivedUtil {
    override def _backToGridState  = new OnPlainGridState
  }
  /** Состояние сразу после клика по карточке в плитке. Отрабатывается запрос, происходит подготовка focused-выдачи. */
  class FocStartingForAd extends StartingForAdStateT {
    override def _focOnAppearState = new FocAppearingState
  }
  class FocAppearingState extends OnAppearStateT {
    override def _focReadyState = new FocOnFocusState
  }
  protected trait ISimpleShift extends super.ISimpleShift {
    override def _shiftRightState = new FocShiftRightState
    override def _shiftLeftState  = new FocShiftLeftState
  }
  protected trait OnFocusStateBaseT extends super.OnFocusStateBaseT with INodeSwitchState with ISimpleShift {
    override def _closingState    = new FocClosingState
  }
  /** Состояние нахождения на какой-то focused-карточке в выдаче. */
  class FocOnFocusState extends OnFocusStateT with OnFocusStateBaseT with IStartFocusOnAdState {
    override def _onTouchStartState = new FocTouchStartState
  }
  /** Состояние закрытия focused-выдачи с возвратом в плитку. */
  class FocClosingState extends FocClosingStateT with IBackToGridState


  /** Реализация интерфейса IShiftDoneState. */
  protected trait IShiftDoneState extends super.IShiftDoneState {
    override protected def _shiftDoneState = new FocOnFocusState
  }

  // Переключение focused-карточек в выдаче.
  /** Состояние перехода на одну карточку вправо. */
  class FocShiftRightState
    extends ShiftRightStateT
      with IShiftDoneState
      with IStartFocusOnAdState

  /** Состояние перехода на одну карточку влево. */
  class FocShiftLeftState
    extends ShiftLeftStateT
      with IShiftDoneState
      with IStartFocusOnAdState


  // Поддержка touch-состояний в focused-выдаче.
  protected trait FocTouchCancelledT
    extends super.FocTouchCancelledT
      with IStartFocusOnAdState
  {
    override def _touchCancelledState = new FocOnFocusState
  }
  class FocTouchStartState extends FocTouchCancelledT with FocOnTouchStartStateT {
    override def _touchShiftState   = new FocTouchShiftState
    override def _touchVScrollState = new FocTouchStartState
  }
  class FocTouchScrollState extends FocTouchCancelledT with FocOnTouchScrollStateT
  class FocTouchShiftState  extends FocOnTouchShiftStateT with ISimpleShift with FocTouchCancelledT

}
