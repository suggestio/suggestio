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
  with PreLoading
  with OnTouch
  with OnTouchPreload
{
  that: ScFsm.type =>

  /** Реализация трейта поддержки ресайза. */
  protected trait IStartFocusOnAdState extends super.IStartFocusOnAdState {
    override def _startFocusOnAdState = new FocStartingForAd
  }

  /** Состояние сразу после клика по карточке в плитке. Отрабатывается запрос, происходит подготовка focused-выдачи. */
  class FocStartingForAd extends StartingForAdStateT with ProcessIndexReceivedUtil {
    override def _focOnAppearState = new FocAppearingState
    override def _backToGridState  = new OnPlainGridState
  }
  class FocAppearingState extends OnAppearStateT {
    override def _focReadyState = new FocOnFocusState
  }
  protected trait ISimpleShift extends super.ISimpleShift {
    override def _shiftRightState = new FocShiftRightState
    override def _shiftLeftState  = new FocShiftLeftState
  }
  protected trait OnFocusStateBaseT extends super.OnFocusStateBaseT with _NodeSwitchState with ISimpleShift {
    override def _closingState    = new FocClosingState
  }
  /** Состояние нахождения на какой-то focused-карточке в выдаче. */
  class FocOnFocusState extends OnFocusStateT with OnFocusStateBaseT with IStartFocusOnAdState {
    override def _leftPreLoadState  = new FocPreLoadLeftState
    override def _rightPreLoadState = new FocPreLoadRightState
    override def _onTouchStartState = new FocTouchStartState
  }
  /** Состояние закрытия focused-выдачи с возвратом в плитку. */
  class FocClosingState extends FocClosingStateT {
    override def _afterDisappearState: FsmState = {
      val sd0 = _stateData
      if (sd0.nav.panelOpened) {
        new OnGridNavReadyState
      } else if (sd0.search.opened) {
        _searchTab2state(sd0)
      } else {
        new OnPlainGridState
      }
    }
  }
  // Переключение focused-карточек в выдаче.
  protected trait SimpleShiftStateT extends super.SimpleShiftStateT with IStartFocusOnAdState {
    override def _shiftDoneState = new FocOnFocusState
  }
  /** Состояние перехода на одну карточку вправо. */
  class FocShiftRightState extends ShiftRightStateT with SimpleShiftStateT
  /** Состояние перехода на одну карточку влево. */
  class FocShiftLeftState  extends ShiftLeftStateT with SimpleShiftStateT

  /** Общий код реализаций focused-preload-состояний. */
  protected trait FocPreLoadingStateT
    extends super.FocPreLoadingStateT
      with OnFocusStateBaseT
      with IStartFocusOnAdState
  {
    override def _preloadDoneState = new FocOnFocusState
  }
  /** Состояние нахождения на крайней правой карточке среди уже подгруженных,
    * но НЕ крайней среди имеющихся в focused-выборке. */
  class FocPreLoadRightState extends FocPreLoadingStateT with FocRightPreLoadingStateT {
    override def _onTouchStartState = new FocTouchShiftPreloadRightState
  }
  class FocPreLoadLeftState  extends FocPreLoadingStateT with FocLeftPreLoadingStateT {
    override def _onTouchStartState = new FocTouchShiftPreloadLeftState
  }

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

  // Touch-навигация началась одновременно с идущим preload. Тут состояния для поддержки подобного явления.
  protected trait FocPreLoadingReceiveStateT extends super.FocPreLoadingReceiveStateT {
    override def _preloadDoneState = new FocTouchShiftState
  }
  class FocTouchShiftPreloadLeftState
    extends OnTouchShiftPreloadLeftStateT
    with ISimpleShift
    with FocTouchCancelledT
    with FocPreLoadingReceiveStateT
  class FocTouchShiftPreloadRightState
    extends OnTouchShiftPreloadRightStateT
    with ISimpleShift
    with FocTouchCancelledT
    with FocPreLoadingReceiveStateT

}
