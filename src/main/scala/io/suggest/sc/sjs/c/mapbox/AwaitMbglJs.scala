package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.vm.maps.MpglAcTok
import io.suggest.sjs.common.fsm.IFsmMsg
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.mapbox.gl.mapboxgl
import io.suggest.sjs.mapbox.gl.window.IMbglWindow
import org.scalajs.dom

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 19:16
  * Description: Аддон поддержки состояний ожидания mapboxgl.js.
  * Считается, что необходимых тег скрипта уже есть в шаблоне.
  */
trait AwaitMbglJs extends MbFsmStub {

  /** Сколько миллисекунд ожидать появление скрипта на странице перед попыткой проверки. */
  def AWAIT_MPGLJS_MS = 250

  /** Трейт для сборки состояния ожидания появления mapboxgl в рантайме. */
  trait AwaitMbglJsStateT extends FsmState {

    /** Сообщение об окончании ожидания скрипта. */
    case object AwaitTimeout extends IFsmMsg

    override def afterBecome(): Unit = {
      super.afterBecome()
      // После входа в это состояние надо узнать, готов ли сейчас mapbox.js к работе.
      val wnd = dom.window: IMbglWindow
      if (wnd.mapboxgl.isEmpty) {
        // Нет готового к работе скрипта. Подождать ещё...
        onJsMissing()

      } else {
        // Есть готовый к работе скрипт на странице.
        onJsFound()
      }
    }

    override def receiverPart: Receive = {
      case AwaitTimeout =>
        become(this)
    }

    /** Действия, когда js отсутствует на странице. */
    def onJsMissing(): Unit = {
      dom.window.setTimeout(
        {() => _sendEventSync(AwaitTimeout) },
        AWAIT_MPGLJS_MS
      )
      log( WarnMsgs.MAPBOXLG_JS_NOT_FOUND )
    }

    /** Действия, когда js найден на странице. */
    def onJsFound(): Unit = {
      // Выставить accessToken.
      for {
        input <- MpglAcTok.find()
        acTok <- input.value
      } {
        mapboxgl.accessToken = acTok
      }

      // Переключиться на след.состояние.
      become(jsReadyState)
    }


    /** На какое состояние переключаться, когда наконец найден скрипт mapboxgl.js на странице. */
    def jsReadyState: State_t

  }

}
