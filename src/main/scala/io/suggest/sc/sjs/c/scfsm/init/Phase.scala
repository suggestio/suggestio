package io.suggest.sc.sjs.c.scfsm.init

import cordova.Cordova
import io.suggest.sc.sjs.c.scfsm.geo.GeoScInit

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.09.15 10:45
 * Description: Трейт фазы стартовой геолокации. Здесь собраны состояния,
 * которые необходимы для начальной геолокации при запуске выдачи.
 */
trait Phase
  extends Init
  with GeoScInit
{

  /** Вход в фазу начальной инициализации.*/
  protected def _initPhaseEnter1st: FsmState = new FirstInitState

  /** Выход из фазы начальной инициализации и геолокации. */
  protected def _initPhaseExit_OnWelcomeGridWait_State: FsmState


  /** Частичная реализация ProcessIndexReceivedUtil в целях дедубликации кода. */
  protected trait ProcessIndexReceivedUtil extends super.ProcessIndexReceivedUtil {
    override protected def _nodeInitWelcomeState = _initPhaseExit_OnWelcomeGridWait_State
  }

  /** Реализация состояния самой первой инициализации. */
  class FirstInitState
    extends FsmEmptyReceiverState
    with NormalInitStateT
    with FirstInitStateT

  /** Реализация состояния типовой инициализации. */
  class NormalInitState
    extends FsmEmptyReceiverState
    with NormalInitStateT


  /**
    * Состояние геоинициализации выдачи.
    * Для cordova надо подождать наступления device ready прежде чем начинать геоинициализацию.
    */
  protected def _geoInitState: FsmState = {
    if ( js.isUndefined( Cordova ) ) {
      new GeoScInitState
    } else {
      new AwaitDeviceReadyState
    }
  }

  /** Состояние ожидания device ready события из cordova. */
  class AwaitDeviceReadyState
    extends EnsureCordovaDeviceReadyStateT
  {
    override def _nextState = new GeoScInitState
  }

  /** Состояние инициализации выдачи на основе геолокации. */
  class GeoScInitState
    extends GeoScInitStateT
    with ProcessIndexReceivedUtil

}
