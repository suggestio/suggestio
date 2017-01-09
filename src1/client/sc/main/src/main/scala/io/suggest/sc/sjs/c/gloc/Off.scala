package io.suggest.sc.sjs.c.gloc

import io.suggest.sc.sjs.m.mgeo.Subscribe
import io.suggest.sjs.common.fsm.signals.IVisibilityChangeSignal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.04.16 19:47
  * Description: Аддон для сборки состояния отключенности от какой-либо работы.
  */
trait Off extends GeoLocFsmStub {

  /** При инициализации состояния погасить все watcher'ы. */
  trait UnwatchAfterBecomeT extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()

      val sd0 = _stateData

      // При переходе в offline необходимо погасить все watcher'ы
      if (sd0.watchers.nonEmpty) {
        _stateData = sd0.copy(
          // Собрать итератор новой карты данных по геолокациям.
          watchers = _clearWatchers(sd0.watchers).toMap
        )
      }
    }
  }


  /** Интерфейс к полю, возвращающему новый инстанс watching-состояния. */
  trait IWatchingState {

    /** Инстанс состояния наблюдения за геолокацией. */
    def _watchingState: FsmState

    def _switchToWatchingState(): Unit = {
      become(_watchingState)
    }

  }


  trait IHandleVisibilityChange extends FsmEmptyReceiverState {
    override def receiverPart: Receive = super.receiverPart.orElse {
      case vc: IVisibilityChangeSignal =>
        _handleVisibilityChanged(vc)
    }

    /** Реакция на изменение состояния visibility страницы. */
    def _handleVisibilityChanged(vc: IVisibilityChangeSignal): Unit
  }


  /** Трейт для сборки off state и sleeping state. */
  trait StandByStateT
    extends FsmState
      with FsmEmptyReceiverState
      with UnwatchAfterBecomeT
      with IWatchingState
      with IHandleVisibilityChange

  /**
    * Трейт состояния отключенности от работы.
    * Состояние реагирует только на сообщение об активации.
    */
  trait OffStateT extends StandByStateT {

    override def _handleSubscribe(s: Subscribe): Unit = {
      super._handleSubscribe(s)
      // Активация, раз уж есть подписчик.
      _switchToWatchingState()
    }

    override def _handleVisibilityChanged(vc: IVisibilityChangeSignal): Unit = {
      // в off-состоянии плевать на изменение видимости страницы текущей.
    }

  }


  /**
    * Трейт состояния сна. Это состояние временной приостановки деятельности,
    * например при уходе на другую вкладку или сокрытия экрана.
    */
  trait SleepingStateT extends StandByStateT {

    /** Реакция на изменение состояния visibility страницы. */
    override def _handleVisibilityChanged(vc: IVisibilityChangeSignal): Unit = {
      if (!vc.isHidden)
        _switchToWatchingState()
    }

  }

}
