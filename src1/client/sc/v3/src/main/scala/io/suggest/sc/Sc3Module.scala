package io.suggest.sc

import com.materialui.MuiTheme
import com.softwaremill.macwire._
import cordova.Cordova
import cordova.plugins.background.geolocation.CdvBgGeo
import cordova.plugins.fetch.CdvPluginFetch
import cordova.plugins.inappbrowser.InAppBrowser
import diode.{Effect, ModelRW}
import io.suggest.common.empty.OptionUtil
import io.suggest.cordova.CordovaConstants
import io.suggest.cordova.background.geolocation.CdvBgGeoLocApi
import io.suggest.cordova.fetch.CdvFetchHttpResp
import io.suggest.dev.{MOsFamilies, MOsFamily}
import io.suggest.geo.{GeoLocApi, Html5GeoLocApi}
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.LoginFormModuleBase
import io.suggest.id.login.m.session.MLogOutDia
import io.suggest.id.login.m.{ILoginFormAction, LoginFormDiConfig}
import io.suggest.lk.IPlatformComponentsModule
import io.suggest.lk.c.CsrfTokenApi
import io.suggest.lk.m.{CsrfTokenEnsure, SessionSet}
import io.suggest.lk.nodes.form.LkNodesModuleBase
import io.suggest.lk.nodes.form.m.NodesDiConf
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.model.{HttpClientConfig, HttpReqData, IMHttpClientConfig}
import io.suggest.routes.IJsRouter
import io.suggest.sc.c.dia.ScNodesDiaAh
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m.{MScRoot, OnlineCheckConn, RouteTo, ScLoginFormShowHide, ScNodesShowHide}
import io.suggest.sc.m.inx.{GetIndex, MScSwitchCtx, ReGetIndex}
import io.suggest.sc.u.Sc3LeafletOverrides
import io.suggest.sc.u.api.{IScStuffApi, IScUniApi, ScAppApiHttp, ScStuffApiHttp, ScUniApi, ScUniApiHttpImpl}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.v._
import io.suggest.sc.v.dia.dlapp._
import io.suggest.sc.v.dia.err._
import io.suggest.sc.v.dia.first._
import io.suggest.sc.v.dia.settings._
import io.suggest.sc.v.grid._
import io.suggest.sc.v.hdr._
import io.suggest.sc.v.inx._
import io.suggest.sc.v.dia.login.ScLoginR
import io.suggest.sc.v.dia.nodes.{ScNodesNeedLoginR, ScNodesR}
import io.suggest.sc.v.menu._
import io.suggest.sc.v.search._
import io.suggest.sc.v.search.found._
import io.suggest.sc.v.snack._
import io.suggest.sc.v.styl._
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.{DoNothing, SioPages}
import japgolly.scalajs.react.{Callback, React}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import org.scalajs.dom.experimental.{RequestInfo, RequestInit}

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

class Sc3Module { outer =>

  import io.suggest.jd.render.JdRenderModule._
  import io.suggest.ReactCommonModule._
  import ScCommonModule._

  lazy val sc3SpaRouter: Sc3SpaRouter = {
    import io.suggest.spa.DiodeUtil.Implicits._
    lazy val rendered: VdomElement = sc3Circuit.wrap( identity(_) )( scRootR.component.apply )

    new Sc3SpaRouter(
      renderSc3F = { sc3Page =>
        sc3Circuit.runEffectAction( RouteTo(sc3Page) )
        rendered
      }
    )
  }

  import sc3SpaRouter.{state => sc3SpaRouterState}
  import sc3SpaRouter.state.routerCtl

  lazy val sc3Circuit: Sc3Circuit = {
    val mkLogOutAh = { modelRW: ModelRW[MScRoot, Option[MLogOutDia]] =>
      ScLoginFormModule.mkLogOutAh[MScRoot]( modelRW )
    }
    wire[Sc3Circuit]
  }
  lazy val sc3LeafletOverrides = new Sc3LeafletOverrides( sc3Circuit )

  /** Функция, возвращающая списки стабильных инстансов реализаций API геолокации. */
  val geoLocApis: () => LazyList[GeoLocApi] = {
    lazy val html5GeoLocApi = wire[Html5GeoLocApi]
    lazy val cdvBgGeoLocApi = new CdvBgGeoLocApi(
      getMessages = () => sc3Circuit.internalsInfoRW.value.commonReactCtx.messages,
    )

    () =>
      val mplat = sc3Circuit.platformRW.value

      if (!mplat.isReady) {
        // Для cordova событие READY ещё не наступило, поэтому CdvBgGeo.isAvailable дёрнуть тут нельзя.
        // Считаем, что CdvBgGeo доступен, когда доступно cordova API.
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

  // React contexts
  lazy val muiThemeCtx = React.createContext[MuiTheme]( null )
  lazy val sc3RouterCtlCtx = React.createContext[RouterCtl[SioPages.Sc3]]( sc3SpaRouter.state.routerCtl )
  lazy val platfromCssCtx = React.createContext[PlatformCssStatic]( sc3Circuit.platformCssRO.value )
  lazy val scCssCtx = React.createContext[ScCss]( sc3Circuit.scCssRO.value )
  lazy val jsRouterOptCtx = React.createContext[Option[IJsRouter]]( sc3Circuit.jsRouterRW.value.jsRouterOpt )


  // Допы для lk-common
  lazy val _getPlatformCss = sc3Circuit.platformCssRO.apply _
  lazy val platformComponents = wire[PlatformComponents]


  // header
  lazy val headerR = wire[HeaderR]
  lazy val logoR = wire[LogoR]
  lazy val menuBtnR = wire[MenuBtnR]
  lazy val nodeNameR = wire[NodeNameR]
  lazy val leftR = wire[LeftR]
  lazy val rightR = wire[RightR]
  lazy val searchBtnR = wire[SearchBtnR]
  lazy val hdrProgressR = wire[HdrProgressR]
  lazy val goBackR = wire[GoBackR]


  // index
  lazy val welcomeR = wire[WelcomeR]


  // grid
  lazy val gridR   = wire[GridR]


  // search
  lazy val searchMapR = wire[SearchMapR]
  lazy val searchR = wire[SearchR]
  lazy val nfListR = wire[NfListR]
  lazy val nfRowsR = wire[NfRowsR]
  lazy val nodesFoundR = wire[NodesFoundR]
  lazy val geoMapOuterR = wire[GeoMapOuterR]


  // menu
  lazy val menuR = wire[MenuR]
  lazy val menuItemR = wire[MenuItemR]
  lazy val versionR = wire[VersionR]
  lazy val enterLkRowR = wire[EnterLkRowR]
  lazy val aboutSioR = wire[AboutSioR]
  lazy val editAdR = wire[EditAdR]
  lazy val dlAppMenuItemR = wire[DlAppMenuItemR]
  lazy val indexesRecentR = wire[IndexesRecentR]
  lazy val scNodesBtnR = wire[ScNodesBtnR]
  lazy val logOutR = wire[LogOutR]


  // dia
  lazy val wzFirstR = wire[WzFirstR]
  lazy val dlAppDiaR = wire[DlAppDiaR]

  // snack
  lazy val scErrorDiaR = wire[ScErrorDiaR]
  lazy val indexSwitchAskR = wire[IndexSwitchAskR]
  lazy val scSnacksR = wire[ScSnacksR]
  lazy val offlineSnackR = wire[OfflineSnackR]

  // dia.settings
  lazy val geoLocSettingR = wire[GeoLocSettingR]
  lazy val blueToothSettingR = wire[BlueToothSettingR]
  lazy val unsafeOffsetSettingR = wire[UnsafeOffsetSettingR]
  lazy val scSettingsDiaR = wire[ScSettingsDiaR]
  lazy val settingsMenuItemR = wire[SettingsMenuItemR]
  lazy val onOffSettingR = wire[OnOffSettingR]
  lazy val blueToothUnAvailInfoR = wire[BlueToothUnAvailInfoR]
  lazy val notificationSettingsR = wire[NotificationSettingsR]

  // sc3
  lazy val scThemes = wire[ScThemes]
  lazy val scRootR = {
    import ScLoginFormModule.logOutDiaR
    wire[ScRootR]
  }


  def sc3UniApi: IScUniApi = {
    import ScHttpConf._
    wire[ScUniApiHttpImpl]
  }
  def scStuffApi: IScStuffApi = {
    import ScHttpConfCsrf._
    wire[ScStuffApiHttp]
  }
  def csrfTokenApi = {
    import ScHttpConf._
    wire[CsrfTokenApi]
  }
  def scAppApiHttp = {
    import ScHttpConf._
    wire[ScAppApiHttp]
  }

  object ScHttpConf {
    /** Сборка HTTP-конфига для всей выдачи, без CSRF. */
    val mkRootHttpClientConfigF = { () =>
      val isCordova = sc3Circuit.platformRW.value.isCordova

      // Замена стандартной, ограниченной в хидерах, fetch на нативный http-client.
      // Это ломает возможность отладки запросов через Chrome WebDev.tools, но даёт предсказуемую работу http-клиента,
      // и главное: поддержку кукисов и произвольных заголовков запроса/ответа, ради чего и есть весь сыр-бор.
      val fetchApi = OptionUtil.maybeOpt(isCordova)(
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
      )

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
        // Поддержка чтения/записи данных сессии без кукисов:
        sessionCookieGet = Option.when(isCordova) { () =>
          sc3Circuit.loginSessionRW.value.cookie.toOption
        },
        sessionCookieSet = Option.when(isCordova) { sessionCookie =>
          sc3Circuit.dispatch( SessionSet( sessionCookie ) )
        },
        // Дефолтовый домен кукисов сессии, т.к. сервер обычно не шлёт домена (чтобы не цеплялось под-доменов).
        cookieDomainDflt = Option.when(isCordova)( ScUniApi.scDomain ),
        fetchApi = fetchApi,
        // okhttp выдаёт ошибку перед запросами: method POST must have request body. Для подавление косяка, выставляем флаг принудительного body:
        // Можно сделать только для osFamily = android, но пока оставляем для всей кордовы.
        forcePostBodyNonEmpty = fetchApi.nonEmpty && isCordova,
      )
    }
  }

  /** Активация поддержки CSRF для всех запросов. */
  object ScHttpConfCsrf {
    val httpClientConfig = () => {
      val v0 = ScHttpConf.mkRootHttpClientConfigF()
      val csrf2 = sc3Circuit.csrfTokenRW.value.toOption
      // Выставить CSRF-токен для всех под-форм, т.к. там почти все запросы требуют CSRF-токена:
      if (v0.csrfToken !=* csrf2)
        (HttpClientConfig.csrfToken set csrf2)(v0)
      else
        v0
    }
  }
  /** HTTP-конфигурация для самостоятельных под-форм выдачи. */
  sealed trait ScHttpConfCsrf extends IMHttpClientConfig {
    override def httpClientConfig = ScHttpConfCsrf.httpClientConfig
  }


  sealed trait ScPlatformComponents extends IPlatformComponentsModule {
    override def getPlatformCss = outer._getPlatformCss
    override def platformComponents = outer.platformComponents
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
          if (CordovaConstants.isCordovaPlatform()) {
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

    override lazy val loginRouterCtl = routerCtl contramap { login: SioPages.Login =>
      // пропихнуть в роутер обновлённую страницу логина.
      (SioPages.Sc3.login set Option(login))(route0)
    }

    override lazy val routerCtlCtx: React.Context[RouterCtl[SioPages.Login]] =
      React.Context( loginRouterCtl )

    override lazy val loginFormCssCtx: React.Context[LoginFormCss] =
      LoginFormModuleBase.circuit2loginCssRCtx( outer.sc3Circuit.scLoginRW.value.ident )

  }
  def getLoginFormCircuit = () => ScLoginFormModule.loginFormCircuit

  lazy val scLoginR = {
    import ScLoginFormModule.loginFormR
    wire[ScLoginR]
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

        /*outer.sc3SpaRouter.state.routerCtl.set(
          SioPages.Sc3(
            nodeId = Some( nodeId ),
          )
        )*/
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
      override def retryErrorFx(effect: Effect): Effect = {
        val retryFx = CsrfTokenEnsure(
          force = true,
          onComplete = Some(effect),
        ).toEffectPure
        OnlineCheckConn.toEffectPure + retryFx
      }
    }
  }
  def getNodesFormCircuit = () => ScNodesFormModule.lkNodesFormCircuit
  lazy val scNodesR = {
    import ScNodesFormModule.lkNodesFormR
    wire[ScNodesR]
  }
  lazy val scNodesNeedLoginR = wire[ScNodesNeedLoginR]

}
