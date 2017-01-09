package io.suggest.ble.beaconer.fsm

import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.fsm.signals.IVisibilityChangeSignal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.10.16 13:03
  * Description: Трейт состояния замороженности в работе.
  */
trait Suspend extends BeaconerFsmStub {

  /** Через сколько миллисекунд считать данные по маячкам невалидными и подлежащими забвению? */
  private def FORGET_BEACON_DATA_AFTER_MS = 12000


  /** Состояние замороженности, т.е. когда bt API погашено и ожидает прихода Resume-сигнала. */
  trait SuspendedStateT extends FsmState with IActiveState {

    /** id таймера сброса накопленных данных о маячках. */
    private var _forgetBeaconDataTimer: Option[Int] = None

    /** Внутренний сигнал о необходимости забыть данные по маячкам. */
    case object ForgetBeaconData


    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()

      // Запустить таймер сброса данных по маячкам, если есть какие-то данные
      val sd0 = _stateData
      if (sd0.beacons.nonEmpty || sd0.envFingerPrint.nonEmpty) {
        val forgetTimerId = DomQuick.setTimeout(FORGET_BEACON_DATA_AFTER_MS) { () =>
          _sendEventSync(ForgetBeaconData)
        }
        _forgetBeaconDataTimer = Some(forgetTimerId)
      }
    }


    override def receiverPart: Receive = {
      val r: Receive = {
        case ForgetBeaconData =>
          _handleForgetBeaconDataTimeout()
      }
      r.orElse(
        super.receiverPart
      )
    }

    /** Реакция на срабатывание таймера забвения накопленных ранее данных по маячкам. */
    def _handleForgetBeaconDataTimeout(): Unit = {
      _forgetBeaconDataTimer = None
      val sd0 = _stateData
      // Сигнатуру профиля оставляем в состоянии, чтобы при возврате на online-состояние сразу происходил сброс плитки.
      _stateData = sd0.withBeacons( Map.empty )
    }

    /** Реакция на сообщение об изменении visibility зависят от состояния. */
    override def _handleVisibilityChange(vc: IVisibilityChangeSignal): Unit = {
      // Если приложение на переднем плане...
      if (!vc.isHidden) {
        for (timerId <- _forgetBeaconDataTimer) {
          DomQuick.clearTimeout(timerId)
        }
        // переключить на онлайновое состояние.
        become( _activeState )

      } else {
        super._handleVisibilityChange(vc)
      }
    }

  }

}
