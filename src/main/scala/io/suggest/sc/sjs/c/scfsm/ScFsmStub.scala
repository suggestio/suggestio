package io.suggest.sc.sjs.c.scfsm

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.c.scfsm.ust.IUrl2State
import io.suggest.sc.sjs.m.magent.{IMScreen, IVpSzChanged, MScreen, VpSzChanged}
import io.suggest.sc.sjs.m.mfsm.signals.KbdKeyUp
import io.suggest.sc.sjs.m.msc.{MScSd, PopStateSignal}
import io.suggest.sc.sjs.vm.nav.nodelist.NlRoot
import io.suggest.sc.sjs.vm.search.SRoot
import io.suggest.sjs.common.fsm._
import io.suggest.sjs.common.model.loc.ILocEnv
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.vsz.ViewportSz
import org.scalajs.dom.KeyboardEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 11:28
 * Description: Заготовка для сборки FSM-частей подсистем.
 */
trait ScFsmStub extends SjsFsm with StateData with DirectDomEventHandlerFsm with IUrl2State {

  override type State_t = FsmState
  override type SD      = MScSd


  /** Детектор данных по экрану. */
  protected def _getScreenOpt: Option[IMScreen] = {
    val vszOpt = ViewportSz.getViewportSize
    if (vszOpt.isEmpty)
      warn( WarnMsgs.NO_SCREEN_VSZ_DETECTED )
    vszOpt.map( MScreen.apply )
  }
  protected def _getScreen: IMScreen = {
    _getScreenOpt.getOrElse {
      // Наврядли этот код будет вызываться когда-либо.
      MScreen(1024, 768)
    }
  }


  /** Трейт для реализации разных логик реакции на изменение размера окна в зависимости от текущего состояния. */
  protected trait HandleViewPortChangedT {

    def _viewPortChanged(): Unit = {
      _viewPortChanged(VpSzChanged)
    }
    /** Дополняемая/настраивамая реакция на сигнал об изменении размеров окна или экрана устройства. */
    def _viewPortChanged(e: IVpSzChanged): Unit = {
      // Обновить данные состояния по текущему экрану.
      val screen = _getScreen
      val sd0 = _stateData
      val sd1 = sd0.withCommon(
        sd0.common.copy(
          screen  = screen
        )
      )
      _stateData = sd1

      // Выполнить какие-то общие для выдачи действия
      // Подправить высоту левой панели...
      for (nlRoot <- NlRoot.find()) {
        nlRoot.reInitLayout(sd1)
      }

      // Подправить высоту правой панели.
      for (sRoot <- SRoot.find()) {
        sRoot.adjust(sd1.common)
      }
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


    /**
      * Реакция на popstate с какими-то данными для выдачи.
      *
      * @param sdNext Распарсенные данные нового состояния из URL.
      */
    def _handleStateSwitch(sdNext: SD): Unit = {
      // Дефолтовое поведение на неотработанные случаи: уйти в инициализацию.
      _nodeReInitState( sdNext )
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
    // Сигнал событий клавиатуры.
    case KbdKeyUp(event) =>
      _state._onKbdKeyUp(event)
    // Сигнал изменения размеров текущего окна.
    case e: IVpSzChanged =>
      _state._viewPortChanged(e)
    // Сигнал навигации по истории браузера.
    case pss: PopStateSignal =>
      _handlePopState(pss)
  }

  /** Ресивер для всех состояний. Неизменен, поэтому [[ScFsm]] он помечен как val. */
  override protected def allStatesReceiver: Receive = {
    _allStatesReceiver
      .orElse( super.allStatesReceiver )
  }

  /** Очень служебное состояние системы, используется когда очень надо. */
  protected[this] class DummyState extends FsmEmptyReceiverState


  /** Доступ к инстансу состояния нахождения на голой плитке. */
  protected def _onPlainGridState: FsmState


  /** Интерфейс для метода, дающего состояние переключения на новый узел.
    * Используется для возможности подмешивания реализации в несколько состояний. */
  protected trait INodeSwitchState {
    protected def _onNodeSwitchState: FsmState
  }


  // API, в т.ч. публичное.

  /** Текущая локация системы. */
  def currLocEnv: ILocEnv = {
    _stateData.locEnv
  }

}
