package io.suggest.sc.sjs.c.scfsm

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.c.mapbox.MbFsm
import io.suggest.sc.sjs.m.mfsm.signals.KbdKeyUp
import io.suggest.sc.sjs.m.mgeo.{IGeoErrorSignal, IGeoLocSignal}
import io.suggest.sc.sjs.m.msc.fsm.MStData
import io.suggest.sjs.common.fsm._
import io.suggest.sjs.common.msg.ErrorMsgs
import org.scalajs.dom.KeyboardEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 11:28
 * Description: Заготовка для сборки FSM-частей подсистем.
 */
trait ScFsmStub extends SjsFsm with StateData with DirectDomEventHandlerFsm {

  override type State_t = FsmState

  override type SD = MStData

  /** Добавление слушателя событий отпускания кнопок клавиатуры в состояние. */
  protected trait FsmState extends super.FsmState with DirectDomEventHandlerDummy {
    /** Переопределяемый метод для обработки событий клавиатуры.
      * По дефолту -- игнорировать все события клавиатуры. */
    def _onKbdKeyUp(event: KeyboardEvent): Unit = {}

    /** Реакция на получение данных геолокации. */
    def _geoLocReceived(gs: IGeoLocSignal): Unit = {
      // Отправить в MbFsm уведомление о наличии геолокации.
      // TODO Не отправлять сигнал, если в состоянии уже такая геопозиция, и сигнал не несёт для системы какой-либо полезной нагрузки.
      MbFsm ! gs
    }

    /** Реакция на получение ошибки получения геолокация. */
    def _geoLocErrorReceived(ge: IGeoErrorSignal): Unit = {
      val e = ge.error
      warn(ErrorMsgs.GEO_LOC_FAILED + " [" + e.code + "] " + e.message)
    }
  }

  /**
   * Если состояние не требует ресивера, то можно использовать этот трейт.
   *
   * Также трейт используется для случаев, когда нужно смешать трейты двух вообще разных состояний,
   * т.е. вместо голого имплемента receiverPart, каждое состояние оверрайдит неабстрактный receiverPart,
   * затем все состояния смешиваются без проблем.
   */
  protected trait FsmEmptyReceiverState extends FsmState {
    override def receiverPart: Receive = PartialFunction.empty
  }

  protected def _allStatesReceiver: Receive = {
    // Реакция на события клавиатуры.
    case KbdKeyUp(event) =>
      _state._onKbdKeyUp(event)
    // Данные по геолокации от браузера могут приходить когда угодно.
    case gs: IGeoLocSignal =>
      _state._geoLocReceived(gs)
    case ge: IGeoErrorSignal =>
      _state._geoLocErrorReceived(ge)
  }

  /** Ресивер для всех состояний. */
  override protected def allStatesReceiver: Receive = {
    _allStatesReceiver
      .orElse( super.allStatesReceiver )
  }


  /** Очень служебное состояние системы, используется когда очень надо. */
  protected[this] class DummyState extends FsmEmptyReceiverState


  /** Интерфейс для метода, дающего состояние переключения на новый узел.
    * Используется для возможности подмешивания реализации в несколько состояний. */
  protected trait INodeSwitchState {
    protected def _onNodeSwitchState: FsmState
  }

  // Раскомментить override become() для логгирования переключения состояний:
  /*override protected def become(nextState: FsmState): Unit = {
    log(_state + " -> " + nextState)
    super.become(nextState)
  }*/

}
