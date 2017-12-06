package io.suggest.sc.sjs.c.gloc

import io.suggest.fsm.StateData
import io.suggest.geo.GeoLocType
import io.suggest.sc.sjs.m.mgeo._
import io.suggest.sjs.common.fsm.{IFsmMsg, SjsFsm}
import io.suggest.sjs.common.vm.wnd.WindowVm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.04.16 16:01
  * Description: stub для сборки кусков FSM, занимающего получением и обработкой гео-сигналов от браузера.
  * Изначально фукционал жил внутри ScFsm, но это вызывало костыли в логике работы переусложненного ScFsm.
  */
trait GeoLocFsmStub extends SjsFsm with StateData {

  override type State_t   = FsmState
  override type SD        = MGeoFsmSd


  /** Трейт для сборки состояний этого FSM. */
  protected[this] trait FsmState extends super.FsmState {

    /** Подписка на события геолокации. */
    def _handleSubscribe(s: Subscribe): Unit = {
      val sd0 = _stateData

      // Создать/обновить подписчика
      val subData1 = sd0.subscribers
        .get(s.receiver)
        .fold(s.data) { _ + s.data }
      // Залить нового/обновлённого подписчика в состояние
      _stateData = sd0.copy(
        subscribers = sd0.subscribers + (s.receiver -> subData1)
      )

      // Послать нулевое уведомление, если запрошено и если есть чем.
      if (s.notifyZero) {
        for {
          (wtype, data) <- sd0.watchers
          lastPos       <- data.lastPos
          if wtype.precision >= s.data.minWatch.precision
        } {
          s.receiver ! GlLocation(lastPos, wtype)
        }
      }
    }

    /** Отказ от подписки на события геолокации. */
    def _handleUnSubscribe(s: UnSubscribe): Unit = {
      // Стереть из карты подписчика.
      val sd0 = _stateData
      for (_ <- sd0.subscribers.get( s.receiver )) {
        val subs2 = sd0.subscribers - s.receiver

        // Обновить данные состояния новой картой подписчиков.
        _stateData = sd0.copy(
          subscribers = subs2
        )
      }
    }

    /** Уведомить подписчиков. */
    def _notifySubscribers(loc: IGeoSignal): Unit = {
      for ( (fsm, subData) <- _stateData.subscribers ) {
        _maybeNotifySubscriber(loc, fsm, subData)
      }
    }

    /** Опциональное уведомление подписчика, если он подходит под сообщение. */
    def _maybeNotifySubscriber(loc: IGeoSignal, fsm: SjsFsm, subData: SubscriberData): Unit = {
      if ((loc.wtype.precision >= subData.minWatch.precision) && (subData.withErrors || loc.isSuccess)) {
        fsm ! loc
      }
    }

    /** Реакция на синхронный запрос любой доступной геолокации. */
    def _getAnyGeoLoc(ask: GetAnyGl): Unit = {
      val wIter = _stateData.watchers
        .iterator
        .filter(_._2.lastPos.nonEmpty)
      val reply: IFsmMsg = if (wIter.nonEmpty) {
        val (wtype, w) = wIter.maxBy(_._1)
        val lastPos = w.lastPos.get
        GlLocation(lastPos, wtype)
      } else {
        GlUnknown
      }
      ask.to ! reply
    }

  }


  trait IBeforeOffline {
    def _beforeOffline(): Unit = {}
  }

  /** Аддон для состояний для отключения от работы когда нет больше подписчиков. */
  trait OffWhenNoSubscribersStateT extends FsmState with IBeforeOffline {

    /** Отказ от подписки на события геолокации. */
    override def _handleUnSubscribe(s: UnSubscribe): Unit = {
      super._handleUnSubscribe(s)
      if (_stateData.subscribers.isEmpty) {
        // Никому больше неинтересна геолокация. Уйти в отключку.
        _beforeOffline()
        become(_offState)
      }
    }

    /** Состояние отключенности от работы. */
    def _offState: FsmState

  }


  protected[this] def _allStatesReceiver: Receive = {
    case s: Subscribe =>
      _state._handleSubscribe(s)
    case us: UnSubscribe =>
      _state._handleUnSubscribe(us)
    case getAny: GetAnyGl =>
      _state._getAnyGeoLoc(getAny)
  }
  override protected val allStatesReceiver: Receive = {
    _allStatesReceiver.orElse {
      super.allStatesReceiver
    }
  }


  protected[this] def _clearWatchers(watchers0: TraversableOnce[(GeoLocType, MglWatcher)]): Iterator[(GeoLocType, MglWatcher)] = {
    for {
      glApi         <- WindowVm().geolocation.iterator
      (wtype, w1)   <- watchers0.toIterator
      w2 = w1.watchId.fold(w1) { watchId =>
        glApi.clearWatch(watchId)
        w1.copy(watchId = None)
      }
      // Если новый MglWatch пустой получился, то отбросить его.
      if w2.nonEmpty
    } yield {
      wtype -> w2
    }
  }

}
