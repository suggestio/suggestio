package io.suggest.sc.controller.dev

import cordova.Cordova
import cordova.plugins.deeplinks.{IUniversalLinks, UniversalLinks}
import cordova.plugins.intent.ICdvIntentShim
import diode.data.Pot
import diode.Implicits._
import io.suggest.cordova.CordovaConstants.{Events => CordovaEvents}
import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRO, ModelRW}
import io.suggest.radio.beacon.{BtOnOff, IBeaconsListenerApi, MBeaconerOpts}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.conf.ConfConst
import io.suggest.event.DomEvents
import io.suggest.cordova.CordovaConstants
import io.suggest.daemon.{BgModeDaemonInit, MDaemonDescr, MDaemonInitOpts}
import io.suggest.dev.{MOsFamilies, MOsFamily, MPlatformS}
import io.suggest.lk.m.SessionRestore
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.model.{GeoLocOnOff, GeoLocTimerStart, LoadIndexRecents, MScRoot, OnlineInit, PauseOrResume, PeripheralStartStop, PlatformReady, RouteTo, ScDaemonDozed, ScLoginFormShowHide, ScNodesShowHide, ScreenResetNow, ScreenResetPrepare, SettingAction, SettingsDiaOpen, SettingsRestore, WithSettings}
import io.suggest.log.Log
import io.suggest.os.notify.{CloseNotify, NotifyStartStop}
import io.suggest.sc.controller.android.ScIntentsAh
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.model.boot.{Boot, BootAfter, MBootServiceIds}
import io.suggest.sc.model.inx.{MScSideBar, MScSideBars, MScSwitchCtx, SideBarOpenClose}
import io.suggest.sc.util.Sc3ConfUtil
import io.suggest.sc.view.toast.ScNotifications
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import io.suggest.sjs.common.vm.evtg.EventTargetVm._
import io.suggest.spa.{DiodeUtil, DoNothing, HwBackBtn, SioPagesUtilJs}
import io.suggest.spa.DiodeUtil.Implicits._
import org.scalajs.dom
import org.scalajs.dom.Event
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.url.bind.QsBindableUtilJs

import scala.concurrent.duration.DurationInt
import scala.scalajs.js.JSON
import scala.util.Try
import scala.util.matching.Regex

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.18 15:48
  * Description: Контроллер платформы, на которой исполняется выдача.
  */
object PlatformAh extends Log {

  /** Статический метод, выполняющий начальную инициализацию данных платформы без серьёзных сайд-эффектов.
    *
    * @return Начальный инстанс MPlatformS.
    */
  def platformInit(): MPlatformS = {
    val doc = dom.document
    // Инициализация для cordova-окружения:
    val isCordova = CordovaConstants.isCordovaPlatform()

    // Определить платформу. https://stackoverflow.com/a/19883965
    // navigator.platform содержат Android, Linux, Linux arm*, и т.д.
    // или iPhone/iPod/iPad [Simulator] для iOS.
    val osFamilyTryOpt = Try {
      (for {
        uaPlatform <- Option( dom.window.navigator.platform ).iterator

        (re, osFamily) <- {
          // ТИП ОБЯЗАТЕЛЕН, иначе компилятор повиснет https://github.com/scala/bug/issues/11914
          val seq: Seq[(Regex, MOsFamily)] = if (scalajs.LinkingInfo.developmentMode) {
            // в dev-режиме надо принудительно вызывать все регэкспы, чтобы выявлять ошибки в любых регэкспах как можно раньше.
            platformAndroidRe :: platformIosRe :: Nil
          } else {
            // в prod-режиме - лениво, упор на эффективность.
            platformAndroidRe #:: platformIosRe #:: LazyList.empty
          }

          seq.iterator
        }

        if re.pattern
          .matcher( uaPlatform )
          .find()
      } yield {
        osFamily
      })
        .nextOption()
    }
    for (ex <- osFamilyTryOpt.failed)
      logger.warn( ErrorMsgs.PLATFORM_ID_FAILURE, ex )

    MPlatformS(
      // Браузеры - всегда готовы. Cordova готова только по внутреннему сигналу готовности.
      isCordova   = isCordova,
      isUsingNow  = !doc.hidden,
      osFamily    = osFamilyTryOpt.toOption.flatten,
    )
  }


  private def platformAndroidRe =
    "(?i)(android|linux(\\s arm)?)".r -> MOsFamilies.Android

  private def platformIosRe =
    "^(iP(hone|[ao]d))".r -> MOsFamilies.Apple_iOS

}


/** Контроллер сигналов платформы. */
final class PlatformAh[M](
                           modelRW              : ModelRW[M, MPlatformS],
                           rootRO               : ModelRO[MScRoot],
                           dispatcher           : Dispatcher,
                           scNotifications      : => ScNotifications,
                           needGeoLocRO         : () => Boolean,
                         )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал об изменении видимости системы.
    case m: PauseOrResume =>
      val v0 = value
      val isUsingNow_LENS = MPlatformS.isUsingNow
      if (isUsingNow_LENS.get(v0) ==* m.isScVisible) {
        // Ничего не изменилось
        noChange

      } else {
        // Изменилось состояние текущей активности приложения. Надо запустить/остановить части системы.
        val v2 = (isUsingNow_LENS replace m.isScVisible)(v0)

        // Если сокрыие выдачи с открытой панелью, то скрыть панель:
        var fxAcc = List.empty[Effect]

        val mroot = rootRO.value
        if (
          needGeoLocRO() &&
          mroot.internals.boot.targets.isEmpty &&
          mroot.dialogs.first.isViewFinished
        ) {
          fxAcc ::= PeripheralStartStop(pauseResume = true).toEffectPure

          // Если уход в фон с активным мониторингом маячков, то надо уйти в бэкграунд.
          if (
            v2.isCordova &&
            (m.isScVisible match {
              // включение: beaconer всегда выключен.
              case true  => m.isScVisible
              // выключение
              case false => (mroot.dev.beaconer.isEnabled contains[Boolean] true)
            })
          ) {
            // Если сокрытие и включён bluetooth-мониторинг, то перейти в background-режим.
            fxAcc ::= ScDaemonDozed(isActive = !m.isScVisible).toEffectPure
          }
        }

        // Если активация приложения, и есть отображаемые нотификации, то надо их затереть.
        if (
          m.isScVisible &&
          mroot.dev.osNotify.hasNotifications
        ) {
          fxAcc ::= CloseNotify(Nil).toEffectPure
        }

        if (v0.isCordova) {
          if (!m.isScVisible) {
            for (p <- mroot.index.panelsOpened)
              fxAcc ::= SideBarOpenClose( p, open = OptionUtil.SomeBool.someFalse ).toEffectPure

          } else if (MPlatformS.lastModifiedMs() - v0.lastModifiedMs > 40000) {
            // Возобновление работы выдачи. Если давно спим, то скрыть диалоги:
            val dia = mroot.dialogs
            if (dia.settings.opened)
              fxAcc ::= SettingsDiaOpen(false).toEffectPure
            if (dia.nodes.opened)
              fxAcc ::= ScNodesShowHide(false).toEffectPure
            if (dia.login.ident.nonEmpty)
              fxAcc ::= ScLoginFormShowHide(false).toEffectPure
          }
        }

        ah.updatedMaybeEffect( v2, fxAcc.mergeEffects )
      }


    // Сигнал о готовности платформы к работе.
    case m: PlatformReady =>
      val v0 = value

      if (
        (m.state ===* Pot.empty) &&
        !v0.isReadyPot.isPending
      ) {
        // Запрос инверсии готовности.
        val isReadyNext = !v0.isReady
        if (isReadyNext) {
          // Инициализация. Подписаться на события:
          val subscribeFx = Effect.action {
            val isReadyPot2 = Try {
              // Может оказаться, что device уже ready:
              val doc = dom.document
              val isCordova = CordovaConstants.isCordovaPlatform()

              if (isCordova) {
                var isReady = false
                // Это кордова-контейнер для веб-приложения.
                // Кордова в момент инициализации обычно не готова ни к чему.

                // Подписка на событие готовности кордовы к работе с железом устройства.
                doc.addEventListener4s( CordovaEvents.DEVICE_READY ) { _: Event =>
                  // TODO Проверять содержимое Event'а? Вдруг, не ready, а ошибка какая-то.
                  // для deviceready функция-листенер может вызваться немедленно. Сразу тут вписать значение в переменную.
                  isReady = true
                  dispatcher.dispatch( PlatformReady( Pot.empty.ready(isReady) ) )
                  // TODO Надо отписаться от ready-события?
                }

                // Сразу выставляем bleAvail=true, т.к. в кордове есть bluetooth примерно всегда. Но если deviceready уже наступило, то проверяем здесь:

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
                  dispatcher.dispatch( HwBackBtn )
                }

                // Вернуть текущее значение isReady, которое зависит от подписки на DEVICE_READY.
                isReady

              } else {
                // Браузер всегда готов к труду и обороне:
                // Это браузер. Он готов сразу. Но надо подписаться на событие Visibility changed
                doc.addEventListener4s( DomEvents.VISIBILITY_CHANGE ) { _: Event =>
                  dispatcher.dispatch(
                    PauseOrResume(
                      isScVisible = !dom.document.hidden
                    )
                  )
                }

                true
              }
            }
              .fold [Pot[Boolean]] (
                v0.isReadyPot
                  .ready(isReadyNext)
                  .fail,
                {isReadyNow =>
                  if (isReadyNow)
                    DiodeUtil.Bool.truePot
                  else
                    v0.isReadyPot.pending()
                }
              )

            PlatformReady( isReadyPot2 )
          }

          // Эффект таймаута, если не получается дождаться готовности.
          val timeoutFx = Effect.action {
            if (modelRW.value.isReady !=* isReadyNext) {
              logger.error( ErrorMsgs.PLATFORM_READY_NOT_FIRED, msg = (m, isReadyNext) )
              PlatformReady( DiodeUtil.Bool(isReadyNext) )
            } else {
              DoNothing
            }
          }
            .after( 7.seconds )

          var fxAcc: Effect = subscribeFx + timeoutFx

          // Если кордова, то восстановить кукисы из хранилища для предстоящих HTTP-запросов
          if (v0.isCordova)
            fxAcc += SessionRestore.toEffectPure

          val v2 = MPlatformS.isReadyPot.modify(_.pending())( v0 )

          updatedSilent( v2, fxAcc )

        } else {
          // TODO Реализовать выключение и зачистку листенеров.
          logger.error( ErrorMsgs.NOT_IMPLEMENTED, msg = m )
          noChange
        }

      } else if (m.state.isPending) {
        // В фоне продолжается процесс обновления готовности...
        val v2 = (MPlatformS.isReadyPot replace m.state)( v0 )
        updatedSilent(v2)

      } else if (m.state.isFailed) {
        // Произошла какая-то ошибка при попытке инициализации.
        val v2 = (MPlatformS.isReadyPot replace m.state.pending())( v0 )

        val retryFx = Effect
          .action( PlatformReady( Pot.empty ) )
          .after( 1.second )

        updatedSilent( v2, retryFx )

      } else if (m.state.isReady) {
        // Готовность успешно перешла в новое качество.
        val isReadyNow = m.state contains[Boolean] true
        // Собираем модификатор значения v0 в несколько шагов. isReady надо выставлять всегда:
        var modF = MPlatformS.isReadyPot replace m.state
        var fxAcc = List.empty[Effect]

        if (isReadyNow) {
          // Возможно, что HwScreenUtil не смогло определить точные размеры экрана, и нужно повторить определение экрана после наступления cordova ready.
          fxAcc ::= ScreenResetPrepare.toEffectPure

          // Определить платформу cordova, если она не была правильно определена на предыдущем шаге.
          if (v0.isCordova) {

            // Subscribe to UniversalLinks events:
            if (IUniversalLinks.isAvailable()) {
              fxAcc ::= Effect.action {
                Try( UniversalLinks.unsubscribeF() )
                UniversalLinks.subscribeF() { eventData =>
                  try {
                    // Zero-cost convertion to immutable-map, compatible with QueryStringBindable:
                    val qs = QsBindableUtilJs.unsafeJSDictionaryAsImmutableMap( eventData.params )
                    val sc3Page = SioPagesUtilJs.parseSc3FromQsTokens( qs )
                    logger.log(s"Universal link found:\n url: ${eventData.url}\n qs = ${JSON.stringify(eventData.params)}\n parsed => $sc3Page")
                    dispatcher.dispatch( RouteTo( sc3Page ) )
                  } catch {
                    case ex: Throwable =>
                      logger.error( ErrorMsgs.URL_PARSE_ERROR, ex, eventData.url )
                  }
                }
                DoNothing
              }
            }

            // Subscribe to android-intent events:
            if (ICdvIntentShim.isApiAvailable())
              fxAcc ::= ScIntentsAh.subscribeIntentsFx( dispatcher )

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
                    modF = modF andThen (MPlatformS.osFamily replace osFamilyOpt)
                  }
                }
              )

            // Принудительно пересчитать экран. В cordova данные экрана определяются через cordova-plugin-device.
            fxAcc ::= ScreenResetNow().toEffectPure

            // Инициализировать поддержку нотификаций:
            fxAcc ::= NotifyStartStop(isStart = true).toEffectPure

            // Инициализация демонизатора:
            if (v0.osFamily.isUseBgModeDaemon) {
              fxAcc ::= Effect.action {
                BgModeDaemonInit(
                  // Это текущая фаза.
                  initOpts = Some( MDaemonInitOpts(
                    //events = MDaemonEvents(
                    //  activated = ScDaemonWorkProcess,
                    //),
                    descr = MDaemonDescr(
                      needBle = true,
                    ),
                    notification = Some( scNotifications.daemonNotifyOpts() ),
                  ))
                )
              }
            }
          }

          // Проверить соединение с интернетом, выведя плашку с ошибкой при необходимости:
          fxAcc ::= OnlineInit(true).toEffectPure

          // Read/restore settings in memory. Early, but still in parallel.
          fxAcc ::= SettingsRestore().toEffectPure

          // Инициализировать список последних узлов, когда платформа будет готова к RW-хранилищу и HTTP-запросам актуализации сохранённого списка.
          // Например, cordova-fetch может быть не готова на iOS до platform-ready.
          fxAcc ::= {
            val jsRouterBootSvcId = MBootServiceIds.JsRouter
            val bootJsRouterFx = Boot( jsRouterBootSvcId :: Nil ).toEffectPure

            val loadRecentsFx = Effect.action {
              val afterFx = Effect.action( LoadIndexRecents(clean = true) )
              BootAfter(
                jsRouterBootSvcId,
                fx        = afterFx,
                ifMissing = Some( afterFx ),
              )
            }

            bootJsRouterFx >> loadRecentsFx
          }
        }

        val v2 = modF(v0)
        ah.updatedSilentMaybeEffect( v2, fxAcc.mergeEffects )

      } else {
        logger.error( ErrorMsgs.UNSUPPORTED_VALUE_OF_ARGUMENT, msg = m )
        noChange
      }


    // On first run, provide limited initialization, instead of big conditions-rich PauseOrResume(true).
    case m: PeripheralStartStop =>
      val isUsingNow = m.isStart getOrElse value.isUsingNow
      var fxAcc = List.empty[Effect]

      for (fx <- _beaconerControlFx( isUsingNow ))
        fxAcc ::= fx

      // Запускать/глушить фоновый GPS-мониторинг:
      for (fx <- _geoLocControlFx( isUsingNow, pauseResume = m.pauseResume ))
        fxAcc ::= fx

      ah.maybeEffectOnly( fxAcc.mergeEffects )

  }


  /** Переключение активированности фоновой геолокации. */
  private def _geoLocControlFx(isScVisible: Boolean, pauseResume: Boolean ): Option[Effect] = {
    val mroot = rootRO()
    val mgl = mroot.dev.geoLoc
    val isActiveNow = mgl.switch.onOff contains[Boolean] true

    // Раняя проверка (без учёта сеттингов) на необходимость какого-либо дальнейшего эффекта:
    val isToEnable0 =
      isScVisible && !isActiveNow &&
      // Надо попытаться всё-равно включить геолокацию в DEV-mode, т.к. браузеры не дают геолокацию без ssl в локалке.
      (Sc3ConfUtil.isDevMode || !mgl.switch.hardLock)

    // Если вроде бы что-то изменяется, то запустить доп.проверку через settings о том, что действительно надо что-либо менять:
    Option.when( isToEnable0 || (!isScVisible && isActiveNow) ) {
      Effect.action {
        SettingAction(
          key = ConfConst.ScSettings.LOCATION_ENABLED,
          fx  = { jsValue =>
            val isToEnable = isToEnable0 && jsValue.asOpt[Boolean].getOrElseFalse

            Option.when( isToEnable || (!isScVisible && isActiveNow) ) {
              lazy val sctx = MScSwitchCtx(
                indexQsArgs = MScIndexArgs(
                  geoIntoRcvr = true,
                  retUserLoc  = false,
                ),
                demandLocTest = pauseResume,
              )

              // Надо запускать обновление выдачи, если включение геолокации и панель карты закрыта.
              val isRunGeoLocInx = isToEnable && !mroot.index.search.panel.opened

              var fxAcc: Effect = Effect.action {
                GeoLocOnOff(
                  enabled  = isScVisible,
                  isHard   = false,
                )
              }

              // При включении - запустить таймер геолокации, чтобы обновился index на новую геолокацию.
              if (isRunGeoLocInx)
              // Передавать контекст, в котором явно указано, что это фоновая проверка смены локации, и всё должно быть тихо.
                fxAcc += GeoLocTimerStart(sctx, allowImmediate = !pauseResume).toEffectPure

              fxAcc
            }
          },
        )
      }
    }
  }


  /** Когда наступает platform ready и BLE доступен,
    * надо попробовать активировать/выключить слушалку маячков BLE и разрешить геолокацию.
    */
  private def _beaconerControlFx( isScVisible: Boolean, plat: MPlatformS = value ): Option[Effect] = {
    val mroot = rootRO()

    // Не выполнять эффектов, если результата от них не будет (без фактической смены состояния или hardOff).
    Option.when(
      // hasBle will be deleted, because filling this flag causes Bluetooth permission request on the screen.
      //(mroot.dev.beaconer.hasBle contains[Boolean] true) &&
      plat.isReady &&
      // Нельзя запрашивать bluetooth до boot'а GeoLoc: BLE scan требует права ACCESS_FINE_LOCATION,
      // приводя к проблеме http://source.suggest.io/sio/sio2/issues/5 , а вместе с
      // плагином cdv-bg-geoloc - к какому-то зависону из-за facade.pause() в геолокации и StackOverflowError в
      // ble-central-плагине при отстутствии прав на FINE_LOCATION.
      // TODO Нельзя запрашивать только ВКЛючение, но не выключенине. На случай каких-то проблем с boot-состоянием.
      mroot.internals.boot.targets.isEmpty
    ) {
      // Проброска через Settings, чтобы гасить жестко выключенный bluetooth.
      Effect.action {
        WithSettings { settingsData =>
          // Если приложение уходит в фон, то nextState может быть переопределено настройками background-сканирования BLE.
          val S = ConfConst.ScSettings

          def __getBool(k: String): Boolean =
            settingsData.data.value
              .get( k )
              .flatMap( _.asOpt[Boolean] )
              .getOrElseFalse

          // Проверить, включён ли bluetooth в настройках?
          val isBleEnabledInSettings = __getBool( S.BLUETOOTH_BEACONS_ENABLED )

          // При сокрытии выдачи: возможна активация фонового сканирования:
          val isToEnable = isBleEnabledInSettings &&
            (isScVisible || __getBool( S.BLUETOOTH_BEACONS_BACKGROUND_SCAN ))

          val fxOpt = Option.when( !(mroot.dev.beaconer.isEnabled contains[Boolean] isToEnable) ) {
            // Требуется изменить текущее состояние сканера маячков.
            Effect.action {
              BtOnOff(
                isEnabled = OptionUtil.SomeBool( isToEnable ),
                opts = MBeaconerOpts(
                  // Не долбить мозг юзеру системным запросом включения bluetooth.
                  askEnableBt   = false,
                  oneShot       = false,
                  scanMode    = IBeaconsListenerApi.ScanMode.BALANCED,
                )
              )
            }
          }

          ActionResult(None, fxOpt)
        }
      }
    }
  }

}
