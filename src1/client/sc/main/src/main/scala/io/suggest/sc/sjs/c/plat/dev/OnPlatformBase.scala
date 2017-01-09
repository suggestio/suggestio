package io.suggest.sc.sjs.c.plat.dev

import io.suggest.sc.sjs.c.plat.PlatformFsmStub
import io.suggest.sc.sjs.m.mdev.{PlatEventListen, PlatformEvents}
import io.suggest.sjs.common.msg.WarnMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.10.16 12:00
  * Description: Аддон для сборки состояний нахождения в активном состоянии.
  */
trait OnPlatformBase extends PlatformFsmStub {

  /** Трейт состояния нахожедния в активномa режиме.  */
  protected trait PlatformActiveStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      // Сигнал подписки/отписки какого-то FSM.
      case pel: PlatEventListen =>
        _handlePlatformEventListen(pel)
    }

    /** Реакция на сигнал подписки/отписки какого-то FSM на platform-сигнал. */
    def _handlePlatformEventListen(pel: PlatEventListen): Unit


    protected[this] def _registerVisibilityChange(): Unit

    protected[this] def _registerMenuBtn(): Unit

    /** Выполнить все необходимые действия, связанные с подпиской на события. */
    protected[this] def _listenEventDo(pel: PlatEventListen, sd0: SD = _stateData): Unit = {
      val sd1 = sd0.subscribers.get(pel.event).fold[SD] {
        // Нет списка подписчиков. Значит, ещё нет подписки на целевое событие.
        if (pel.subscribe) {
          if (pel.event == PlatformEvents.VISIBILITY_CHANGE) {
            _registerVisibilityChange()
          } else if (pel.event == PlatformEvents.MENU_BTN) {
            _registerMenuBtn()
          } else {
            throw new UnsupportedOperationException(WarnMsgs.UNSUPPORTED_EVENT + " " + pel.event)
          }
          sd0.withSubscribers(
            subs2 = sd0.subscribers + (pel.event -> List(pel.listener))
          )
        } else {
          LOG.warn( WarnMsgs.UNSUPPORTED_EVENT, msg = pel.event )
          sd0
        }

      } { listeners =>
        val hasCurrListener = listeners.contains(pel.listener)
        if (!hasCurrListener && pel.subscribe) {
          sd0.withSubscribers(
            sd0.subscribers + (pel.event -> (pel.listener :: listeners))
          )
        } else if (hasCurrListener && !pel.subscribe) {
          // Удаляем из подписки. Даже если получился пустой список - это норм,
          // этот FSM будет знать, что уже подписался на данное системные событие.
          val ls2 = listeners.filter(_ != pel.listener)
          sd0.withSubscribers(
            sd0.subscribers + (pel.event -> ls2)
          )
        } else {
          if (hasCurrListener && pel.subscribe) {
            // Подавляем дублирующуюся подписку на события.
            LOG.warn(WarnMsgs.EVENT_ALREADY_LISTENED_BY, msg = pel.event + " " + pel.listener)
          } else {
            // Нельзя удалить то, чего нет. Клиент не подписан на данное событие.
            LOG.log( WarnMsgs.CANNOT_UNSUBSCRIBE_NOT_SUBSCRIBED, msg = pel.event + " " + pel.listener + " " + listeners.mkString(", ") )
          }
          sd0
        }
      }

      _stateData = sd1
    }

  }

}
