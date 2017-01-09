package io.suggest.sc.sjs.c.plat.dev

import _root_.cordova.CordovaConstants.Events
import io.suggest.sc.sjs.m.mdev.{PlatEventListen, PlatformEvents}
import io.suggest.sc.sjs.m.mdev.cordova.{ICordovaFsmMsg, MenuButton, Pause, Resume}
import io.suggest.sjs.common.fsm.signals.{IMenuBtnClick, IVisibilityChangeSignal, PlatformReady}
import io.suggest.sjs.common.vm.doc.DocumentVm
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.10.16 15:44
  * Description: Поддержка cordova в качестве платформы.
  * Подписка на ready-события тупо форвардится в cordova, т.к. та сама неплохо справляется с этим делом.
  * visibility-change-события реализуются через pause/resume.
  */
trait Cordova
  extends OnPlatformBase
  with NotReady
{
  me =>

  /** Трейт для сборки состояния ожидания готовности cordova. */
  protected trait CordovaNotReadyStateT extends NotReadyStateT {

    /** Инстанс функции-листенера, который можно отписать  */
    private val _platformReadyListenerF: js.Function1[Event,_] = {
      { e: Event =>
        me ! PlatformReady(e)
      }
    }

    /** Подписаться на события готовности платформы к работе. */
    override def _subscribePlatformReady(): Unit = {
      dom.document
        .addEventListener(PlatformEvents.E_DEVICE_READY, _platformReadyListenerF)
    }

    /** Отписаться от событий готовности платформы к работе. */
    override def _unSubscribePlatformReady(): Unit = {
      dom.document
        .removeEventListener(PlatformEvents.E_DEVICE_READY, _platformReadyListenerF)
    }

  }


  /** Трейт для состояния взаимодействия с cordova. */
  protected trait CordovaStateT extends PlatformActiveStateT {

    override def receiverPart: Receive = {
      val r: Receive = {
        // Сигнал от cordova об отправке приложения в фон или восстановления активности приложения:
        case vc: IVisibilityChangeSignal with ICordovaFsmMsg =>
          _handleVisibilityChange(vc)

        case m: IMenuBtnClick =>
          _handleMenuBtnClick(m)
      }
      r.orElse( super.receiverPart )
    }


    override protected[this] def _registerVisibilityChange(): Unit = {
      val docVm = DocumentVm()
      docVm.addEventListener( Events.PAUSE )( _signalCallbackF(Pause) )
      docVm.addEventListener( Events.RESUME )( _signalCallbackF(Resume) )
    }

    override protected[this] def _registerMenuBtn(): Unit = {
      DocumentVm()
        .addEventListener( Events.MENU_BUTTON )( _signalCallbackF(MenuButton) )
    }

    /** Реакция на сигнал отправки приложения в фон. */
    def _handleVisibilityChange(vc: IVisibilityChangeSignal): Unit = {
      _broadcastToAll(PlatformEvents.VISIBILITY_CHANGE, vc)
    }

    // TODO Почему-то на андройде меню не работает в cordova.
    def _handleMenuBtnClick(m: IMenuBtnClick): Unit = {
      _broadcastToAll(PlatformEvents.MENU_BTN, m)
    }

    /** Реакция на сигнал подписки/отписки какого-то FSM на platform-сигнал. */
    override def _handlePlatformEventListen(pel: PlatEventListen): Unit = {
      if (pel.event == PlatformEvents.E_DEVICE_READY) {
        if (pel.subscribe) {
          pel.listener ! PlatformReady(null)
        }

      } else {
        // Остальные события подхватываются прямо тут
        _listenEventDo(pel)
      }
    }

  }

}
