package io.suggest.sc.sjs.c.scfsm.geo

import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.c.scfsm.node.Index
import io.suggest.sc.sjs.m.mgeo.GeoTimeout
import org.scalajs.dom

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.09.15 9:50
 * Description: Поддержка таймаута геолокации и переключения на следующее состояние.
 */
trait Timeout extends ScFsmStub with Index {

  /** Аддон для сборки состояния с установкой гео-таймера. */
  trait InstallGeoTimeoutStateT extends FsmState {

    /** Таймаут срабатывания гео-таймера в миллисекундах. */
    def GEO_TIMEOUT_MS: Int

    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить таймер
      val timerId = dom.window.setTimeout(
        {() => _sendEventSyncSafe( GeoTimeout ) },
        GEO_TIMEOUT_MS
      )
      // Сохранить id таймера в состояние.
      val sd0 = _stateData
      _stateData = sd0.copy(
        geo = sd0.geo.copy(
          timer = Some(timerId)
        )
      )
    }

  }


  /** Интерфейс для переключения состояния по наступлению гео-таймаута. */
  trait IGeoTimeout {
    /** На какое состояние переключаться по таймауту геолокации при отсутствии index'а в состоянии? */
    def _geoTimeoutNeedIndexState: FsmState
  }

  /** Аддон для сборки состояния ожидания геотаймера. */
  trait ListenGeoTimerStateT extends FsmEmptyReceiverState with IGeoTimeout with ProcessIndexReceivedUtil {

    override def receiverPart: Receive = super.receiverPart orElse {
      case GeoTimeout =>
        val sd0 = _stateData
        val sd1 = sd0.copy(
          geo = sd0.geo.copy(
            timer = None
          )
        )
        _stateData.geo.lastInx match {
          case Some(minx) =>
            _stateData = sd1
            _nodeIndexReceived(minx)
          case None =>
            become( _geoTimeoutNeedIndexState, sd1 )
        }
    }

  }

}
