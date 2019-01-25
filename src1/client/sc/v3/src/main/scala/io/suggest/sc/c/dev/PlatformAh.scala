package io.suggest.sc.c.dev

import io.suggest.cordova.CordovaConstants.{Events => CordovaEvents}
import diode.{ActionHandler, ActionResult, Dispatcher, ModelRW}
import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.common.event.DomEvents
import io.suggest.cordova.CordovaConstants
import io.suggest.dev.MPlatformS
import io.suggest.sc.m.{PauseOrResume, SetPlatformReady}
import io.suggest.sjs.common.log.Log
import japgolly.univeq._
import io.suggest.sjs.common.vm.evtg.EventTargetVm._
import org.scalajs.dom
import org.scalajs.dom.Event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.18 15:48
  * Description: Контроллер платформы, на которой исполняется выдача.
  */
object PlatformAh {

  def isBleAvailCheck(): Boolean =
    IBleBeaconsApi.detectApis().nonEmpty

  /** Статический метод, выполняющий начальную инициализацию платформы.
    * Можно вызывать только один раз во время запуска.
    * Повторный вызов приведёт к некорректной работе системы.
    *
    * @return Начальный инстанс MPlatformS.
    */
  def platformInit(dispatcher: Dispatcher): MPlatformS = {
    val doc = dom.document

    // Переменная, намекающая на видимость выдачи в текущий момент.
    // По идее, тут всегда true, т.к. инициализация идёт, когда выдача открыта на экране.
    var isUsingNow = true
    var isReady = false
    var isBleAvail = false

    // Инициализация для cordova-окружения:
    val isCordova = CordovaConstants.isCordovaPlatform()
    if (isCordova) {
      // Это кордова-контейнер для веб-приложения.
      // Кордова в момент инициализации обычно не готова ни к чему.
      isReady = false
      isBleAvail = isBleAvailCheck()

      // Подписка на событие готовности кордовы к работе с железом устройства.
      doc.addEventListener4s( CordovaEvents.DEVICE_READY ) { _: Event =>
        // TODO Проверять содержимое Event'а? Вдруг, не ready, а ошибка какая-то.
        // для deviceready функция-листенер может вызваться немедленно. Сразу тут вписать значение в переменную.
        isReady = true
        dispatcher.dispatch( SetPlatformReady )
        // TODO Надо отписаться от ready-события?
      }

      // Подписка на события видимости (открытости приложения на экране устройства).
      def __subscribeForCordovaVisibility(eventType: String, isScVisible: Boolean): Unit = {
        doc.addEventListener4s( eventType ) { _: Event =>
          dispatcher.dispatch( PauseOrResume(isScVisible = isScVisible) )
        }
      }
      __subscribeForCordovaVisibility( CordovaEvents.PAUSE, isScVisible = false )
      __subscribeForCordovaVisibility( CordovaEvents.RESUME, isScVisible = true )

      // TODO Подписаться на события кнопки menu, которая не работает?
    }

    // Инициализация для обычного браузера:
    val isPlainBrowser = !isCordova
    if (isPlainBrowser) {
      // Уточнить значение видимости:
      isUsingNow = !doc.hidden
      // Браузер всегда готов к труду и обороне:
      isReady = true

      // Пока нет поддержки для BLE в браузере. TODO добавить поддержку web-bluetooth, когда она появится.
      isBleAvail = false

      // Это браузер. Он готов сразу. Но надо подписаться на событие Visibility changed
      doc.addEventListener4s( DomEvents.VISIBILITY_CHANGE ) { _: Event =>
        dispatcher.dispatch(
          PauseOrResume(
            isScVisible = !dom.document.hidden
          )
        )
      }
    }

    MPlatformS(
      // Браузеры - всегда готовы. Cordova готова только по внутреннему сигналу готовности.
      isReady     = isReady,
      isCordova   = isCordova,
      isUsingNow  = isUsingNow,
      hasBle  = isBleAvail
    )
  }

}


/** Контроллер сигналов платформы. */
class PlatformAh[M](
                     modelRW    : ModelRW[M, MPlatformS]
                   )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал об изменении видимости системы.
    case m: PauseOrResume =>
      val v0 = value
      if (v0.isUsingNow ==* m.isScVisible) {
        noChange
      } else {
        val v2 = v0.withIsUsingNow( m.isScVisible )
        updated(v2)
      }

    // Сигнал о готовности платформы к работе.
    case SetPlatformReady =>
      val v0 = value
      if (v0.isReady) {
        noChange
      } else {
        var v2 = v0.withIsReady( true )

        // Проверить, не изменились ли ещё какие-то платформенные флаги?
        val bleAvail2 = PlatformAh.isBleAvailCheck()
        if (v2.hasBle !=* bleAvail2)
          v2 = v2.withHasBle( bleAvail2 )

        // TODO Проверять ble avail?

        updated(v2)
      }

  }

}
