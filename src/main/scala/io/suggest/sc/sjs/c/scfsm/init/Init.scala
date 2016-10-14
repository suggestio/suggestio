package io.suggest.sc.sjs.c.scfsm.init

import cordova.CordovaConstants
import io.suggest.common.event.WndEvents
import io.suggest.sc.sjs.c.scfsm.ScFsmStub
import io.suggest.sc.sjs.c.scfsm.ust.IUrl2State
import io.suggest.sc.sjs.m.magent.{OrientationChange, WndResize}
import io.suggest.sc.sjs.m.msc.MUrlUtil
import io.suggest.sc.sjs.util.router.srv.SrvRouter
import io.suggest.sc.sjs.v.global.DocumentView
import io.suggest.sc.sjs.vm.{SafeDoc, SafeWnd}
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.fsm.signals.CordovaDeviceReady
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.vm.doc.DocumentVm
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.15 17:25
 * Description: Поддержка состояний инициализации выдачи.
 * Это обычно синхронные состояния, которые решают на какое состояние переключаться при запуске.
 */
trait Init extends ScFsmStub with IUrl2State {

  /** Трейт для сборки состояния самой первой инициализации.
    * Тут происходит normal-init, но дополнительно может быть строго одноразовая логика.
    * Состояние случается только один раз и синхронно. */
  protected trait FirstInitStateT extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()

      // Запускаем инициализацию js-роутера в фоне.
      SrvRouter.getRouter()

      // TODO Это нужно вообще или нет?
      DocumentView.initDocEvents()

      // Добавляем реакцию на изменение размера окна/экрана.
      val w = SafeWnd
      w.addEventListener(WndEvents.RESIZE)( _signalCallbackF(WndResize) )
      w.addEventListener(WndEvents.ORIENTATION_CHANGE)( _signalCallbackF(OrientationChange) )

      // Провоцируем сохранение в состояние FSM текущих параметров экрана.
      _viewPortChanged()
    }
  }


  /** Трейт синхронной инициализации выдачи и ScFsm.
    * От First-init отличается тем, что логика тут только повторяемая.
    * Эта инициализация может вызываться более одного раза для в случае подавления ошибок. */
  protected trait NormalInitStateT extends FsmState {

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Десериализовывать состояние из текущего URL и перейти к нему.
      val scSdOpt = _parseFromUrlHash()
        .orElse {
          // Отработать содержимое canonical URL, может там лежат данные состояния.
          DocumentVm().head
            .links
            .filter(_.isCanonical)
            .flatMap(_.href)
            .flatMap(MUrlUtil.getUrlHash)
            .flatMap(_parseFromUrlHash)
            .toStream
            .headOption
        }
      _runInitState( scSdOpt)
    }

  }


  /**
    * Ожидание наступления device ready state.
    *
    * Это нужно сделать для доступа к bluetooth и геолокации внутри cordova.
    * Если не дожидаться, то геолокация просто молча не будет работать,
    * а блютус -- вообще выдавать js undefined вместо API.
    */
  protected trait EnsureCordovaDeviceReadyStateT extends FsmEmptyReceiverState {

    /** Сигнал о таймауте ожидания device ready. */
    case object DevRdyTimeOut

    /** id таймера таймаута ожидания сигнала CordovaDeviceReady. */
    private var _devRdyTimerId: Int = _

    override def afterBecome(): Unit = {
      super.afterBecome()

      // Подписаться на события device ready от cordova
      SafeDoc.addEventListener( CordovaConstants.EVENT_DEVICE_READY ) { event: Event =>
        _sendEventSync( CordovaDeviceReady(event) )
      }

      // Лимитируем ожиданием события. На debug-билде device ready наступало в течение ~500 мс.
      _devRdyTimerId = DomQuick.setTimeout(1500) { () =>
        _sendEventSync( DevRdyTimeOut )
      }
    }


    override def receiverPart: Receive = super.receiverPart.orElse {

      // Поступил ожидаемый сигнал device ready.
      case cdr: CordovaDeviceReady =>
        _handleDeviceReady()

      // Таймаут ожидания device ready.
      case DevRdyTimeOut =>
        _handleTimeOut()
    }

    /** Реакция на сигнал о наступлении готовности девайса к работе. */
    def _handleDeviceReady(): Unit = {
      DomQuick.clearTimeout( _devRdyTimerId )
      _becomeNextState()
    }

    /** Реакция на таймаут ожидания готовности девайса. */
    def _handleTimeOut(): Unit = {
      warn( WarnMsgs.CORDOVA_DEVICE_READY_WAIT_TIMEOUT )
      _becomeNextState()
    }

    def _becomeNextState() = become(_nextState)
    def _nextState: FsmState

  }

}
