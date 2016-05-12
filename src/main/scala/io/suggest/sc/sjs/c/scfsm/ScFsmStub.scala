package io.suggest.sc.sjs.c.scfsm

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.m.magent.{IVpSzChanged, MScreen, VpSzChanged}
import io.suggest.sc.sjs.m.mfsm.signals.KbdKeyUp
import io.suggest.sc.sjs.m.msc.fsm.MStData
import io.suggest.sc.sjs.vm.nav.nodelist.NlRoot
import io.suggest.sc.sjs.vm.search.SRoot
import io.suggest.sjs.common.fsm._
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.vsz.ViewportSz
import org.scalajs.dom.KeyboardEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 11:28
 * Description: Заготовка для сборки FSM-частей подсистем.
 */
trait ScFsmStub extends SjsFsm with StateData with DirectDomEventHandlerFsm {

  override type State_t = FsmState
  override type SD      = MStData


  /** Трейт для реализации разных логик реакции на изменение размера окна в зависимости от текущего состояния. */
  protected trait HandleViewPortChangedT {

    def _viewPortChanged(): Unit = {
      _viewPortChanged(VpSzChanged)
    }
    /** Дополняемая/настраивамая реакция на сигнал об изменении размеров окна или экрана устройства. */
    def _viewPortChanged(e: IVpSzChanged): Unit = {

      // Обновить данные состояния по текущему экрану.
      val vszOpt = ViewportSz.getViewportSize
      if (vszOpt.isEmpty)
        warn( WarnMsgs.NO_SCREEN_VSZ_DETECTED )
      val screenOpt = vszOpt.map( MScreen.apply )
      val sd0 = _stateData
      val sd1 = sd0.copy(
        screen  = screenOpt
      )

      // Выполнить какие-то общие для выдачи действия
      // Подправить высоту левой панели...
      for (nlRoot <- NlRoot.find()) {
        nlRoot.reInitLayout(sd1)
      }

      // Подправить высоту правой панели.
      for (mscreen <- sd1.screen; sRoot <- SRoot.find()) {
        sRoot.adjust(mscreen, sd1.browser)
      }

      _stateData = sd1
    }

  }


  /** Добавление слушателя событий отпускания кнопок клавиатуры в состояние. */
  protected trait FsmState
    extends super.FsmState
      with DirectDomEventHandlerDummy
      with HandleViewPortChangedT
  {
    /** Переопределяемый метод для обработки событий клавиатуры.
      * По дефолту -- игнорировать все события клавиатуры. */
    def _onKbdKeyUp(event: KeyboardEvent): Unit = {}
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
    case e: IVpSzChanged =>
      _state._viewPortChanged(e)
  }

  /** Ресивер для всех состояний. Неизменен, поэтому [[ScFsm]] он помечен как val. */
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
