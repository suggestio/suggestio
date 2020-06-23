package io.suggest.sc.c.dev

import cordova.Cordova
import io.suggest.cordova.CordovaConstants.{Events => CordovaEvents}
import diode.{ActionHandler, ActionResult, Dispatcher, ModelRW}
import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.event.DomEvents
import io.suggest.cordova.CordovaConstants
import io.suggest.dev.{MOsFamilies, MOsFamily, MPlatformS}
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.m.{PauseOrResume, SetPlatformReady}
import io.suggest.log.Log
import io.suggest.sc.m.inx.{MScSideBar, MScSideBars, SideBarOpenClose}
import japgolly.univeq._
import io.suggest.sjs.common.vm.evtg.EventTargetVm._
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.util.Try
import scala.util.matching.Regex

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.18 15:48
  * Description: Контроллер платформы, на которой исполняется выдача.
  */
object PlatformAh extends Log {

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


      // Подписка на события кнопок menu, search
      def __subscribeCdvSideBarBtn( eventType: String, bar: MScSideBar ): Unit = {
        doc.addEventListener4s( eventType ) { _: Event =>
          dispatcher.dispatch( SideBarOpenClose(bar, None) )
        }
      }
      __subscribeCdvSideBarBtn( CordovaEvents.MENU_BUTTON, MScSideBars.Menu )
      // TODO Search: + сразу фокус на поле ввода запроса?
      __subscribeCdvSideBarBtn( CordovaEvents.SEARCH_BUTTON, MScSideBars.Search )

      // Подписка на события back-button - отсутствует, поэтому работа идёт через Sc3SpaRouter.
      // Так-то оно работает по умолчанию, но нужно отработать сворачивание приложения в фон, когда некуда уходить.
      doc.addEventListener4s( CordovaEvents.BACK_BUTTON ) { _: Event =>
        // Если в History API больше некуда идти, то надо сворачиваться.
        val h = dom.window.history
        val hLen0 = h.length
        h.back()
        println( s"${getClass.getSimpleName} BACK btn click $hLen0=>${h.length}" )
      }
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

    // Определить платформу. https://stackoverflow.com/a/19883965
    // navigator.platform содержат Android, Linux, Linux arm*, и т.д.
    // или iPhone/iPod/iPad [Simulator] для iOS.
    val osFamilyTryOpt = Try {
      // В try завёрнут весь код, чтобы избежать не всегда явных проблем с ошибками в regexp'ах в некоторых браузерах.
      Option( dom.window.navigator.platform )
        // Для браузера: распарсить navigator.platform.
        .flatMap { platform =>
          ((
            if (scalajs.LinkingInfo.developmentMode) {
              // в dev-режиме надо принудительно вызывать все регэкспы, чтобы выявлять ошибки в любых регэкспах как можно раньше.
              platformAndroidRe :: platformIosRe :: Nil
            } else {
              // в prod-режиме - лениво, упор на эффективность.
              platformAndroidRe #:: platformIosRe #:: LazyList.empty
            }
            // Ниже задан тип, т.к. опять всплывает баг полиморфизма коллекций в scalac, и без явного типа он тут виснет навсегда.
          ): Seq[(Regex, MOsFamily)])
          // Имитируем ленивый и неленивый полный проход регэкспов через filter+headOption, вместо collectFirst.
          // НЕЛЬЗЯ использовать withFilter() или for-if, чтобы не нарушить замысел.
          // В случае LazyList тут будет ленивый рассчёт с наименьшим кол-вом итераций.
            .filter { reAndOs =>
              reAndOs._1.pattern
                .matcher( platform )
                .find()
            }
            .map(_._2)
            .headOption
        }
    }
    for (ex <- osFamilyTryOpt.failed)
      logger.warn( ErrorMsgs.PLATFORM_ID_FAILURE, ex )

    MPlatformS(
      // Браузеры - всегда готовы. Cordova готова только по внутреннему сигналу готовности.
      isReady     = isReady,
      isCordova   = isCordova,
      isUsingNow  = isUsingNow,
      hasBle      = isBleAvail,
      osFamily    = osFamilyTryOpt.toOption.flatten,
    )
  }


  private def platformAndroidRe =
    "(?i)(android|linux(\\s arm)?)".r -> MOsFamilies.Android

  private def platformIosRe =
    "^(iP(hone|[ao]d))".r -> MOsFamilies.Apple_iOS

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
      val isUsingNow_LENS = MPlatformS.isUsingNow
      if (isUsingNow_LENS.get(v0) ==* m.isScVisible) {
        noChange
      } else {
        val v2 = (isUsingNow_LENS set m.isScVisible)(v0)
        updated(v2)
      }

    // Сигнал о готовности платформы к работе.
    case SetPlatformReady =>
      val v0 = value
      val isReady_LENS = MPlatformS.isReady
      if ( isReady_LENS.get(v0) ) {
        noChange

      } else {
        // Собираем модификатор значения v0 в несколько шагов. isReady надо выставлять всегда:
        var modF = isReady_LENS set true

        // Проверить, не изменились ли ещё какие-то платформенные флаги?
        val bleAvail2 = PlatformAh.isBleAvailCheck()
        if (v0.hasBle !=* bleAvail2)
          modF = modF andThen MPlatformS.hasBle.set( bleAvail2 )

        // Определить платформу cordova, если она не была правильно определена на предыдущем шаге.
        Try( Cordova.platformId )
          .fold[Unit](
            {ex =>
              logger.warn( ErrorMsgs.PLATFORM_ID_FAILURE, ex )
            },
            {cordovaPlatformStr =>
              val osFamilyOpt = MOsFamilies
                .values
                .find { osFamily =>
                  osFamily.cordovaPlatformId contains[String] cordovaPlatformStr
                }
              for {
                osFamily <- osFamilyOpt
                if !(v0.osFamily contains[MOsFamily] osFamily)
              } {
                modF = modF andThen MPlatformS.osFamily.set( osFamilyOpt )
              }
            }
          )

        val v2 = modF(v0)
        updated(v2)
      }

  }

}
