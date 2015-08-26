package io.suggest.sc.sjs.c.scfsm.init

import io.suggest.sc.sjs.c.scfsm.geo.GeoInit
import io.suggest.sc.sjs.m.magent.MScreen
import io.suggest.sc.sjs.m.magent.vsz.ViewportSz
import io.suggest.sc.sjs.v.global.DocumentView
import io.suggest.sjs.common.msg.WarnMsgs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.15 17:25
 * Description: Поддержка состояний инициализации выдачи.
 * Это обычно синхронные состояния, которые решают на какое состояние переключаться при запуске.
 */
trait Init extends JsRouterInit with GeoInit {

  /** Трейт для сборки состояния самой первой инициализации.
    * Тут происходит normal-init, но дополнительно может быть строго одноразовая логика.
    * Состояние случается только один раз и синхронно. */
  protected trait FirstInitStateT extends JsRouterInitStartStateT with NormalInitStateT {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // TODO Это нужно вообще или нет?
      DocumentView.initDocEvents()
    }
  }


  /** Трейт синхронной инициализации выдачи и ScFsm.
    * От First-init отличается тем, что логика тут только повторяемая.
    * Эта инициализация может вызываться более одного раза для в случае подавления ошибок. */
  protected trait NormalInitStateT extends FsmEmptyReceiverState {

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Инициализировать состояние.
      val vszOpt = ViewportSz.getViewportSize
      if (vszOpt.isEmpty)
        warn( WarnMsgs.NO_SCREEN_VSZ_DETECTED )
      val sd1 = _stateData.copy(
        screen = vszOpt.map( MScreen.apply )
      )
      // TODO Десериализовывать состояние из URL и выбирать состояние.
      val nextState = _geoAskState
      // Сразу переключаемся на новое состояние.
      become(nextState, sd1)
    }

    /** Состояние инициализации решает, что необходимо запросить геолокацию у браузера.
      * Такое происходит, когда нет данных состояния в URL. */
    protected def _geoAskState: FsmState
  }


  /** Частичная реализация GeoWaitStateT для нужд инициализации. */
  protected trait InitGeoWaitStateT extends GeoWaitStateT {
    protected def _geoFinishedState: FsmState

    override protected def _geoFailedState = _geoFinishedState
    override protected def _geoReadyState  = _geoFinishedState
  }


  /** Трейт для сборки состояния ожидания завершения инициализации js-роутера
    * и запроса (запуск + ожидании) геолокации одновременно. */
  protected trait Init_JsRouterWait_GeoAskWait_StateT
    extends JsRouterInitReceiveT
    with BssGeoAskStartT
    with InitGeoWaitStateT


  /** Трейт шага инициализации, когда jsRouter уже готов, а геолокация пока висит. */
  protected trait Init_GeoWait_StateT
    extends FsmEmptyReceiverState
    with InitGeoWaitStateT

}
