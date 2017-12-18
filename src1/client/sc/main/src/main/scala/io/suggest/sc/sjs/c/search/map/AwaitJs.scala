package io.suggest.sc.sjs.c.search.map

import io.suggest.msg.WarnMsgs
import io.suggest.sc.sjs.vm.maps.MpglAcTok
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.fsm.IFsmMsg
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
trait AwaitJs extends GeoLoc with Early {

  /** Сколько миллисекунд ожидать появление скрипта на странице перед попыткой проверки. */
  def AWAIT_MPGLJS_MS = 250

  /** Сообщение об окончании ожидания скрипта. */
  private case object AwaitTimeout extends IFsmMsg

  /** Трейт для сборки состояния ожидания появления mapboxgl в рантайме. */
  trait MapAwaitJsStateT extends HandleGeoLocStateT with HandleAll2Early {

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
      val r: Receive = {
        // Пора попробовать инициализировать повторно.
        case AwaitTimeout =>
          // TODO LogBecome: В ходе инициализации *дважды* пишется "MbFsm: AwaitMbglJsState -> JsInitializingState"
          become(this)
      }
      r.orElse(super.receiverPart)
    }


    /** Действия, когда js отсутствует на странице. */
    def onJsMissing(): Unit = {
      DomQuick.setTimeout(AWAIT_MPGLJS_MS) { () =>
        _sendEventSync(AwaitTimeout)
      }
      LOG.log( WarnMsgs.MAPBOXLG_JS_NOT_FOUND )
    }

    /** Действия, когда js найден на странице. */
    def onJsFound(): Unit = {
      // Выставить accessToken.
      for {
        input <- MpglAcTok.find()
        acTok <- input.value
      } {
        mapboxgl.accessToken = acTok
        // Этот input более не нужен, токен извлечен и перенесён в состояние.
        input.remove()
      }

      // Переключиться на след.состояние.
      become(_jsReadyState)
    }


    /** На какое состояние переключаться, когда наконец найден скрипт mapboxgl.js на странице. */
    def _jsReadyState: FsmState

  }

}
