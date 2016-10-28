package io.suggest.sc.sjs.c.plat.dev

import io.suggest.sc.sjs.m.mdev.{PlatEventListen, PlatformEvents}
import io.suggest.sjs.common.fsm.signals.{PlatformReady, VisibilityChange}
import io.suggest.sjs.common.vm.doc.DocumentVm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.10.16 12:06
  * Description: Интерфейс для взаимодействия с нижележащей платформой.
  * Впервые появился при разделении на browser и cordova.
  */
trait Browser extends OnPlatformBase {

  /**
    * Состояние активности системы в браузере.
    * Нужно лениво подписаться на visibility change events.
    */
  protected trait BrowserStateT extends PlatformActiveStateT {

    override def receiverPart: Receive = {
      val r: Receive = {
        case vc: VisibilityChange =>
          _handleVisibilityChanged(vc)
      }
      r.orElse( super.receiverPart )
    }


    override protected[this] def _registerVisibilityChange(): Unit = {
      DocumentVm()
        .addEventListener( DocumentVm.VISIBILITY_CHANGE )( _signalCallbackF(VisibilityChange) )
    }

    /** Реакция на сигнал подписки/отписки какого-то FSM на platform-сигнал. */
    override def _handlePlatformEventListen(pel: PlatEventListen): Unit = {
      val sd0 = _stateData

      // Отработать подписку на событие готовности платформы, которое неактуально.
      if (pel.event == PlatformEvents.E_DEVICE_READY) {
        // Браузер всегда готов. Сразу ответить, не подписывать ничего.
        if (pel.subscribe) {
          pel.listener ! PlatformReady(null)
        }

      } else {
        _listenEventDo(pel, sd0)
      }
    }


    /** Реакция на событие изменения видимости текущей страницы (вкладки). */
    def _handleVisibilityChanged(vc: VisibilityChange): Unit = {
      // Разослать всем подписчикам дубликат сообщения от браузера.
      for {
        listeners <- _stateData.subscribers.get( PlatformEvents.VISIBILITY_CHANGE )
        l         <- listeners
      } {
        l ! vc
      }
    }

  }

}
