package io.suggest.sc.sjs.c.plat.dev

import _root_.cordova.CordovaConstants.Events
import io.suggest.sc.sjs.m.mdev.{PlatEventListen, PlatformEvents}
import io.suggest.sc.sjs.m.mdev.cordova.{ICordovaFsmMsg, Pause, Resume}
import io.suggest.sjs.common.fsm.signals.{IVisibilityChangeSignal, PlatformReady}
import io.suggest.sjs.common.vm.doc.DocumentVm
import org.scalajs.dom.Event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.10.16 15:44
  * Description: Поддержка cordova в качестве платформы.
  * Подписка на ready-события тупо форвардится в cordova, т.к. та сама неплохо справляется с этим делом.
  * visibility-change-события реализуются через pause/resume.
  */
trait Cordova extends OnPlatformBase {

  /** Трейт для состояния взаимодействия с cordova. */
  protected trait CordovaStateT extends PlatformActiveStateT {

    override def receiverPart: Receive = {
      val r: Receive = {
        // Сигнал от cordova об отправке приложения в фон или восстановления активности приложения:
        case vc: IVisibilityChangeSignal with ICordovaFsmMsg =>
          _handleVisibilityChange(vc)
      }
      r.orElse( super.receiverPart )
    }


    override protected[this] def _registerVisibilityChange(): Unit = {
      val docVm = DocumentVm()
      docVm.addEventListener( Events.PAUSE )( _signalCallbackF(Pause) )
      docVm.addEventListener( Events.RESUME )( _signalCallbackF(Resume) )
    }

    /** Реакция на сигнал отправки приложения в фон. */
    def _handleVisibilityChange(vc: IVisibilityChangeSignal): Unit = {
      for {
        listeners <- _stateData.subscribers.get( PlatformEvents.VISIBILITY_CHANGE )
        l         <- listeners
      } {
        l ! vc
      }
    }

    /** Реакция на сигнал подписки/отписки какого-то FSM на platform-сигнал. */
    override def _handlePlatformEventListen(pel: PlatEventListen): Unit = {
      val devReadyEvent = PlatformEvents.E_DEVICE_READY
      if (pel.event == devReadyEvent) {
        if (pel.subscribe) {
          // Пробрасываем подписку в cordova runtime, она сама ответит, если уже готова или будет готова.
          // https://cordova.apache.org/docs/en/latest/cordova/events/events.html#deviceready
          // > Any event handler registered after the deviceready event fires has its callback function called immediately.
          val l = pel.listener
          DocumentVm().addEventListener( devReadyEvent ) { e: Event =>
            l ! PlatformReady(e)
          }
        }

      } else {
        // Остальные события подхватываются прямо тут
        _listenEventDo(pel)
      }
    }

  }

}
