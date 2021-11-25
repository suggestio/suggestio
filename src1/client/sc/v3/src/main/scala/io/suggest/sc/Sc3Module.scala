package io.suggest.sc

import com.github.zpao.qrcode.react.{ReactQrCode, ReactQrCodeProps}
import com.softwaremill.macwire._
import cordova.Cordova
import cordova.plugins.fetch.CdvPluginFetch
import cordova.plugins.inappbrowser.InAppBrowser
import diode.react.ModelProxy
import diode.Effect
import io.suggest.ble.cdv.CdvBleBeaconsApi
import io.suggest.common.empty.OptionUtil
import io.suggest.cordova.CordovaConstants
import io.suggest.cordova.background.fetch.CdvBgFetchAh
import io.suggest.cordova.fetch.CdvFetchHttpResp
import io.suggest.daemon.{MDaemonState, MDaemonStates}
import io.suggest.geo.{GeoLocApi, Html5GeoLocApi}
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.LoginFormModuleBase
import io.suggest.id.login.m.{ILoginFormAction, LoginFormDiConfig}
import io.suggest.leaflet.LeafletGeoLocAh
import io.suggest.lk.m.{CsrfTokenEnsure, SessionSet}
import io.suggest.lk.nodes.form.LkNodesModuleBase
import io.suggest.lk.nodes.form.m.NodesDiConf
import io.suggest.maps.c.{MapCommonAh, RcvrMarkersInitAh}
import io.suggest.maps.u.{AdvRcvrsMapApiHttpViaUrl, DistanceUtilLeafletJs}
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.node.{MNodeType, MNodeTypes}
import io.suggest.nfc.{FakeNfcApiImpl, INfcApi}
import io.suggest.nfc.cdv.CordovaNfcApi
import io.suggest.nfc.web.WebNfcApi
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.cookie.MCookieState
import io.suggest.proto.http.model.{HttpClientConfig, HttpReqData, IHttpCookies, IMHttpClientConfig, MCsrfToken}
import io.suggest.qr.QrCodeRenderArgs
import io.suggest.radio.beacon.{BeaconerAh, IBeaconsListenerApi}
import io.suggest.sc.ads.MScNodeMatchInfo
import io.suggest.sc.controller.dev.OnLineAh
import io.suggest.sc.controller.dia.{ScLoginDiaAh, ScNodesDiaAh}
import io.suggest.sc.controller.search.ScMapDelayAh
import io.suggest.sc.controller.showcase.ScHwButtonsAh
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.model.grid.{GridAfterUpdate, GridLoadAds}
import io.suggest.sc.model.in.MScDaemon
import io.suggest.sc.model.{MScRoot, OnlineCheckConn, ScDaemonWorkProcess, ScLoginFormShowHide, ScNodesShowHide}
import io.suggest.sc.model.inx.{GetIndex, MScSwitchCtx, ReGetIndex}
import io.suggest.sc.model.search.MGeoTabS
import io.suggest.sc.util.api.ScUniApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.view.dia.login.ScLoginR
import io.suggest.sc.view.dia.nodes.{ScNodesNeedLoginR, ScNodesR}
import io.suggest.sc.view.search._
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.CircuitUtil.mkLensZoomRW
import io.suggest.spa.DiodeUtil.Implicits.EffectsOps
import io.suggest.spa.{DAction, DoNothing, SioPages}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.wifi.CdvWifiWizard2BeaconsApi
import japgolly.scalajs.react.{Callback, React}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import org.scalajs.dom.experimental.{RequestInfo, RequestInit}

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.07.17 23:03
  * Description: DI-модуль линковки самого верхнего уровня sc3.
  * Все аргументы-зависимости объявлены и линкуются внутри тела модуля.
  */
object Sc3Module {
  /** Для возможности доступа из статических API тут есть переменная, где можно сохранить текущий инстанс. */
  var ref: Sc3Module = null
}

final class Sc3Module extends ScCommonModule { outer =>

  import io.suggest.ReactCommonModule._

  //lazy val sc3LeafletOverrides = new Sc3LeafletOverrides( sc3Circuit )

  lazy val html5GeoLocApi = wire[Html5GeoLocApi]
  //lazy val cdvBgGeoLocApi = new CdvBgGeoLocApi(
  //  getMessages = () => sc3Circuit.internalsInfoRW.value.commonReactCtx.messages,
  //)


  override lazy val sc3Circuit: ScCommonCircuit = new ScCommonCircuitA { circuit =>
    private def scNodesDia = new ScNodesDiaAh(
      modelRW           = scNodesRW,
      getNodesCircuit   = () => ScNodesFormModule.lkNodesFormCircuit,
      sc3Circuit        = this,
    )
    override def scNodesDiaAh: HandlerFunction = scNodesDia
    override def scLoginDiaAh = new ScLoginDiaAh(
      modelRW = scLoginRW,
      getLoginFormCircuit = () => ScLoginFormModule.loginFormCircuit,
    )

    override def geoLocApis() = {
      val mplat = platformRW.value

      if (!mplat.isReady) {
        // Для cordova событие READY ещё не наступило, поэтому CdvBgGeo.isAvailable дёрнуть тут нельзя.
        // Считаем, что CdvBgGeo доступен, когда доступно cordova API.
        logger.error( ErrorMsgs.PLATFORM_READY_NOT_FIRED, msg = ("geoLocApis", mplat) )
        LazyList.empty

      } else {
        var apisAcc = LazyList.empty[GeoLocApi]

        // Добавить HTML5 geolocation API в начало списка. Т.к. cdv-bg-geo вторичен.
        val apis22 = LazyList.cons[GeoLocApi]( html5GeoLocApi, apisAcc )
        apisAcc = apis22

        /*
        if (
          mplat.isCordova &&
          CdvBgGeo.isAvailableAndCordovaReady() &&
          // cdv-background-geolocation: как-то странно он на android работает, поэтому активируем его только для iOS:
          (mplat.osFamily contains[MOsFamily] MOsFamilies.Apple_iOS)
        ) {
          val apis33 = LazyList.cons[GeoLocApi]( cdvBgGeoLocApi, apisAcc )
          apisAcc = apis33
        }
        */

        apisAcc
      }
    }

    override def beaconApis() = {
      val plat = platformRW.value
      var acc = LazyList.empty[IBeaconsListenerApi]

      if (plat.isCordova) {
        for {
          osFamily <- plat.osFamily
          wifiAdp <- CdvWifiWizard2BeaconsApi.forOs( osFamily )
        } {
          val accNoWifi = acc
          acc = wifiAdp #:: accNoWifi
        }

        // Prepend Bluetooth scanning API:
        val accNoBle = acc
        acc = CdvBleBeaconsApi.forOs( plat.osFamily ) #:: accNoBle
      }

      // TODO else: WebBluetoothBeaconsApi
      acc
    }

    override def logOutAh: HandlerFunction =
      ScLoginFormModule.mkLogOutAh[MScRoot]( logOutRW )

    override def leafletGeoLocAhOpt = Some {
      new LeafletGeoLocAh[MScRoot](
        modelRW     = scGeoLocRW,
        geoLocApis  = geoLocApis,
      )
    }

    // Activate geo.map controls support:
    override val mmapsRW = super.mmapsRW
    override val mapDelayRW = super.mapDelayRW
    override val mapAhs: HandlerFunction = {
      val mapCommonAh = new MapCommonAh(
        mmapRW = mmapsRW
      )
      val scMapDelayAh = new ScMapDelayAh(
        modelRW = mapDelayRW
      )
      foldHandlers( mapCommonAh, scMapDelayAh )
    }

    override def scHwButtonsAh = new ScHwButtonsAh(
      modelRW = rootRW,
    )
    override def daemonSleepTimerAh: HandlerFunction = {
      new CdvBgFetchAh(
        dispatcher = circuit,
        modelRW = mkLensZoomRW( daemonRW, MScDaemon.cdvBgFetch )
      )
      //if ( CordovaConstants.isCordovaPlatform() /*&& CordovaBgTimerAh.hasCordovaBgTimer()*/ ) {
      //new CordovaBgTimerAh(
      //  dispatcher = this,
      //  modelRW    = mkLensZoomRW( daemonRW, MScDaemon.cdvBgTimer ),
      //)
      /*} else {
          // TODO Не ясно, надо ли это активировать вообще? Может выкинуть (закомментить) этот контроллер? И его модель-состояние следом.
          new HtmlBgTimerAh(
            dispatcher = this,
            modelRW    = mkLensZoomRW( daemonRW, MScDaemon.htmlBgTimer ),
          )
        }*/
    }
    override def onLineAh: HandlerFunction = new OnLineAh(
      modelRW       = onLineRW,
      dispatcher    = this,
      retryActionRO = scErrorDiaRW.zoom( _.flatMap(_.retryAction) ),
      platformRO    = platformRW,
    )

    override val beaconerAh: HandlerFunction = new BeaconerAh(
      modelRW     = beaconerRW,
      dispatcher  = this,
      bcnsIsSilentRO = scNodesRW.zoom(!_.opened),
      beaconApis  = beaconApis,
      onNearbyChange = Some { (nearby0, nearby2) =>
        var fxAcc = List.empty[Effect]

        // Отправить эффект изменения в списке маячков
        if (scNodesRW.value.opened) {
          fxAcc ::= Effect.action {
            scNodesDia.handleBeaconsDetected()
            DoNothing
          }
        }

        val canUpdateBleGrid = inxStateRO.value.isBleGridAds

        /** Экшен для перезапроса с сервера только BLE-карточек плитки. */
        def _gridBleReloadFx: Effect = {
          Effect.action {
            GridLoadAds(
              clean         = true,
              ignorePending = true,
              silent        = OptionUtil.SomeBool.someTrue,
              onlyMatching  = Some( MScNodeMatchInfo(
                ntype = Some( MNodeTypes.RadioSource.BleBeacon ), // TODO Replace ntype with "MNodeTypes.RadioSource" after app v5.0.3 installed (including GridAh & ScAdsTile).
              )),
            )
          }
        }

        if (daemonRW.value.state contains[MDaemonState] MDaemonStates.Work) {
          // Если что-то изменилось, то надо запустить обновление плитки.
          def finishWorkProcFx: Effect = {
            Effect.action( ScDaemonWorkProcess(isActive = false) )
          }

          if ( !canUpdateBleGrid || (nearby0 ===* nearby2) ) {
            // Ничего не изменилось: такое возможно при oneShot-режиме. Надо сразу деактивировать режим демонизации.
            fxAcc ::= finishWorkProcFx
          } else {
            // Что-то изменилось в списке маячков. Надо запустить обновление плитки.
            fxAcc ::= Effect.action {
              GridAfterUpdate(
                effect = finishWorkProcFx,
              )
            }
            fxAcc ::= _gridBleReloadFx
          }
          fxAcc.mergeEffects

        } else {
          // Логика зависит от режима, который сейчас: работа демон или обычный режим вне демона.
          // Подписываемся на события изменения списка наблюдаемых маячков.
          OptionUtil.maybeOpt( nearby0 !===* nearby2 ) {
            val mroot = rootRW.value

            val gridUpdFxOpt = if (mroot.index.resp.isPending) {
              // Сигнал пришёл, когда уже идёт запрос плитки/индекса, то надо это уведомление закинуть в очередь.
              Option.when(
                !mroot.grid.afterUpdate.exists {
                  case gla: GridLoadAds =>
                    gla.onlyMatching.exists { om =>
                      om.ntype.exists { ntype =>
                        MNodeTypes.lkNodesUserCanCreate contains[MNodeType] ntype
                      }
                    }
                  case _ => false
                }
              ) {
                // Нужно забросить в состояние плитки инфу о необходимости обновится после заливки исходной плитки.
                (Effect.action( GridAfterUpdate( _gridBleReloadFx )) :: fxAcc)
                  .mergeEffects
                  .get
              }

            } else if (!canUpdateBleGrid) {
              // Выставлен запрет на изменение плитки.
              None

            } else {
              // Надо запустить пересборку плитки. Без Future, т.к. это - callback-функция.
              (_gridBleReloadFx :: fxAcc).mergeEffects
            }

            gridUpdFxOpt
          }
        }
      },
    )

    override def rcvrMarkersInitAh = new RcvrMarkersInitAh(
      modelRW         = rcvrRespRW,
      api             = advRcvrsMapApi,
      argsRO          = rcvrsMapUrlRO,
      isOnlineRoOpt   = Some( onLineRW.zoom(_.isOnline) ),
    )
    def advRcvrsMapApi = wire[AdvRcvrsMapApiHttpViaUrl]

  }


  lazy val searchMapR = wire[SearchMapR]
  override def mkSearchMapF: Option[ModelProxy[MGeoTabS] => VdomNode] =
    Some( searchMapR.component.apply )

  /** Use react-qrcode for QR-code rendering. */
  override def qrCodeRenderF: Option[ModelProxy[QrCodeRenderArgs] => VdomNode] = {
    Some { argsProxy =>
      val args = argsProxy.value
      ReactQrCode(
        new ReactQrCodeProps {
          override val value = args.data
          override val renderAs = ReactQrCode.RenderAs.SVG
          override val size = args.sizePx
        }
      )
    }
  }


  /** Cordova + Browser implementation for HttpClientConfig maker. */
  override protected def _httpConfMaker: IScHttpConf = new IScHttpConf {
    override def mkHttpClientConfig(csrf: Option[MCsrfToken]): HttpClientConfig = {
      val isCordova = sc3Circuit.platformRW.value.isCordova

      // Замена стандартной, ограниченной в хидерах, fetch на нативный http-client.
      // Это ломает возможность отладки запросов через Chrome WebDev.tools, но даёт предсказуемую работу http-клиента,
      // и главное: поддержку кукисов и произвольных заголовков запроса/ответа, ради чего и есть весь сыр-бор.
      val httpFetchApi = OptionUtil.maybeOpt( isCordova ) {
        // Try: какие-то трудности на фоне JSGlobalScope при недоступности cordova-API в браузере.
        CdvPluginFetch.cordovaFetchUnd
          .toOption
          .map { cdvFetchF =>
            // Оборачиваем функцию, чтобы выдавала стандартный Response() с поддержкой clone() и прочего.
            {(reqInfo: RequestInfo, reqInit: RequestInit) =>
              cdvFetchF(reqInfo, reqInit)
                .toFuture
                .map( CdvFetchHttpResp )
            }
          }
      }

      HttpClientConfig(
        baseHeaders = HttpReqData.mkBaseHeaders(
          xrwValue = {
            val xrw = HttpConst.Headers.XRequestedWith
            var hdrValue = xrw.XRW_VALUE
            if (isCordova)
              hdrValue += xrw.XRW_APP_SUFFIX
            hdrValue
          },
        ),
        cookies = Option.when( isCordova ) {
          new IHttpCookies {
            override def sessionCookieGet(): Option[MCookieState] =
              sc3Circuit.loginSessionRW.value.cookie.toOption
            override def sessionCookieSet(cookieState: MCookieState): Unit =
              sc3Circuit.dispatch( SessionSet( cookieState ) )
            // Дефолтовый домен кукисов сессии, т.к. сервер обычно не шлёт домена (чтобы не цеплялось под-доменов).
            override def cookieDomainDefault(): Option[String] =
              ScUniApi.scDomain()
          }
        },
        fetchApi = httpFetchApi,
        // okhttp выдаёт ошибку перед запросами: method POST must have request body. Для подавление косяка, выставляем флаг принудительного body:
        // Можно сделать только для osFamily = android, но пока оставляем для всей кордовы.
        forcePostBodyNonEmpty = httpFetchApi.nonEmpty && isCordova,
        // For cordova: add some language-related information. In browser, cookie is set by server during lang-switch POST, not here.
        language = OptionUtil.maybeOpt( isCordova )( sc3Circuit.languageOrSystemRO.value ),
      )
    }
  }


  /** HTTP-конфигурация для самостоятельных под-форм выдачи. */
  sealed trait ScHttpConfCsrf extends IMHttpClientConfig {
    override def httpClientConfig = ScHttpConfCsrf.httpClientConfig
  }


  def beaconsListenApis: () => LazyList[IBeaconsListenerApi] = { () =>
    val plat = sc3Circuit.platformRW.value
    var acc = LazyList.empty[IBeaconsListenerApi]

    if (plat.isCordova) {
      for {
        osFamily <- plat.osFamily
        wifiAdp <- CdvWifiWizard2BeaconsApi.forOs( osFamily )
      } {
        val accNoWifi = acc
        acc = wifiAdp #:: accNoWifi
      }

      // Prepend Bluetooth scanning API:
      val accNoBle = acc
      acc = CdvBleBeaconsApi.forOs( plat.osFamily ) #:: accNoBle
    }

    // TODO else: WebBluetoothBeaconsApi

    acc
  }


  // login
  object ScLoginFormModule
    extends LoginFormModuleBase
    with ScPlatformComponents
  {

    override val diConfig: LoginFormDiConfig = new LoginFormDiConfig with ScHttpConfCsrf {

      override def onClose(): Option[Callback] = {
        val cb = Callback {
          sc3Circuit.dispatch( ScLoginFormShowHide(visible = false) )
        }
        Some(cb)
      }

      override def onRedirect(onAction: ILoginFormAction, external: Boolean, rdrUrl: => String): Effect = {
        // TODO external=true: Нужно фрейм открывать поверх выдачи. Возможно, задействовать cordova-plugin-inappbrowser .
        Effect.action {
          if (CordovaConstants.isCordovaPlatform() || sc3SpaRouter.state.isCanonicalRouteHasNodeId) {
            // В текущей форме cordova: тихо перезагрузить текущий Index с сервера без welcome, выставив в состояние залогиненность.
            val act = ReGetIndex()
            sc3Circuit.dispatch( act )
          } else {
            // Обычный редирект - в браузере.
            DomQuick.goToLocation( rdrUrl )
          }

          // в LoginForm делать ничего не надо:
          DoNothing
        }
      }

      override def onLogOut(): Option[Effect] = {
        val reGetIndexFx = ReGetIndex().toEffectPure
        Some( reGetIndexFx )
      }

      override def showInfoPage: Option[String => Callback] = {
        Option.when(
          CordovaConstants.isCordovaPlatform() &&
            JsApiUtil.isDefinedSafe( Cordova.InAppBrowser )
        ) { url =>
          // TODO Проводить вызов через контроллер страницы, т.к. браузер открывается не сразу, и надо отображать крутилку, блокируя интерфейс.
          Callback {
            Cordova.InAppBrowser.open( url, target = InAppBrowser.Target.SYSTEM )
          }
        }
      }

    }

    private def route0: SioPages.Sc3 =
      sc3Circuit.currRouteRW.value getOrElse SioPages.Sc3.empty

    override lazy val loginRouterCtl = sc3SpaRouter.state.routerCtl contramap { login: SioPages.Login =>
      // пропихнуть в роутер обновлённую страницу логина.
      (SioPages.Sc3.login replace Option(login))(route0)
    }

    override lazy val routerCtlCtx: React.Context[RouterCtl[SioPages.Login]] =
      React.Context( loginRouterCtl )

    override lazy val loginFormCssCtx: React.Context[LoginFormCss] =
      LoginFormModuleBase.circuit2loginCssRCtx( outer.sc3Circuit.scLoginRW.value.ident )

  }
  override def logOutDiaROpt = Some( ScLoginFormModule.logOutDiaR )
  override def scLoginROpt = Some {
    import ScLoginFormModule.loginFormR
    wire[ScLoginR]
  }


  override lazy val nfcApiOpt: Option[INfcApi] = {
    var acc = LazyList.empty[INfcApi]

    if (scalajs.LinkingInfo.developmentMode) {
      // The last element: fake API implementation
      val acc0 = acc
      acc = new FakeNfcApiImpl #:: acc0
    }

    // Try to detect API, depending on current device.
    val isCordova = CordovaConstants.isCordovaPlatform()
    Try {
      (if (isCordova) {
        // Cordova API:
        sc3Circuit.platformRW
          .value.osFamily
          .map ( CordovaNfcApi.apply )
      } else {
        // Try Web NFC in browser:
        Option( new WebNfcApi )
      })
        .filter(_.isApiAvailable())
    }
      .fold(
        {ex =>
          logger.error( ErrorMsgs.NFC_API_ERROR, ex, isCordova )
        },
        {nfcApiOpt =>
          for (nfcApi <- nfcApiOpt) {
            val acc1 = acc
            acc = nfcApi #:: acc1
          }
        }
      )

    acc.headOption
  }


  /** Поддержка lk-nodes. */
  object ScNodesFormModule
    extends LkNodesModuleBase
    with ScPlatformComponents
  {
    override val diConfig: NodesDiConf = new NodesDiConf with ScHttpConfCsrf {
      override def circuitInit() = ScNodesDiaAh.scNodesCircuitInit( sc3Circuit )
      override def openNodeScOpt = Some { nodeId =>
        // Надо открыть выдачу, но чтобы кнопка "Назад" возвращала в исходный диалог узлов и возвращала предыдущее состояние выдачи.
        Callback {
          outer.sc3Circuit.dispatch(
            GetIndex(
              MScSwitchCtx(
                indexQsArgs = MScIndexArgs(
                  nodeId = Some(nodeId),
                ),
                showWelcome = false,
                afterIndex = Some( Effect.action(
                  ScNodesShowHide( false, keepState = true )
                )),
                afterBack = Some( Effect.action {
                  ScNodesShowHide( true )
                }),
                viewsAction = MScSwitchCtx.ViewsAction.PUSH,
              )
            )
          )
        }
      }
      override def closeForm = Some( Callback(
        outer.sc3Circuit.dispatch( ScNodesShowHide(false) )
      ))
      /** Ссылки в ЛК необходимо показывать только в браузере, но не в Cordova,
        * т.к. переброс из приложения в браузер не готов, и мобильная вёрстка ЛК тоже отсутствует. */
      override def showLkLinks() = outer.sc3Circuit.platformRW.value.isBrowser
      override def isUserLoggedIn() = outer.sc3Circuit.indexRW.value.isLoggedIn
      override def needLogInVdom(chs: VdomNode*) =
        outer.sc3Circuit.wrap( identity(_) )( scNodesNeedLoginR.component(_)(chs: _*) )
      override def withBleBeacons = true

      override def onErrorFxOpt: Option[Effect] =
        Some( _toSc3CircuitFx(OnlineCheckConn) )

      override def retryErrorFx(ex: Throwable, effect: Effect): Effect = {
        var actionsAcc: List[DAction] = (
          CsrfTokenEnsure(
            force = true,
            onComplete = Some(effect),
          ) ::
            Nil
          )

        if ( OnlineCheckConn.maybeNeedCheckConnectivity(ex) )
          actionsAcc ::= OnlineCheckConn

        actionsAcc
          .iterator
          .map( _toSc3CircuitFx )
          .mergeEffects
          .get
      }
      override def nfcApi = nfcApiOpt
      override def contextAdId() = {
        outer.sc3Circuit
          .focusedAdRO
          .value
          .flatMap(_.id)
      }
      override def appOsFamily = sc3Circuit.platformRW.value.osFamily
    }
  }
  override def scNodesROpt = Some {
    import ScNodesFormModule.lkNodesFormR
    wire[ScNodesR]
  }
  lazy val scNodesNeedLoginR = wire[ScNodesNeedLoginR]


  override def distanceUtilJsOpt = Some( wire[DistanceUtilLeafletJs] )

}
