package io.suggest.sc

import com.materialui.MuiTheme
import com.softwaremill.macwire._
import diode.Effect
import io.suggest.common.empty.OptionUtil
import io.suggest.cordova.CordovaConstants
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.LoginFormModuleBase
import io.suggest.id.login.m.{ILoginFormAction, LoginFormDiConfig}
import io.suggest.lk.nodes.form.LkNodesModuleBase
import io.suggest.lk.nodes.form.m.NodesDiConf
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.proto.http.model.{HttpClientConfig, IMHttpClientConfig}
import io.suggest.routes.IJsRouter
import io.suggest.sc.c.dia.ScNodesDiaAh
import io.suggest.sc.m.{RouteTo, ScLoginFormShowHide, ScNodesShowHide}
import io.suggest.sc.m.boot.MSpaRouterState
import io.suggest.sc.m.inx.ReGetIndex
import io.suggest.sc.u.api.ScAppApiHttp
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
import io.suggest.sc.v.dia.nodes.ScNodesR
import io.suggest.sc.v.menu._
import io.suggest.sc.v.search._
import io.suggest.sc.v.search.found._
import io.suggest.sc.v.snack._
import io.suggest.sc.v.styl._
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.{DoNothing, SioPages}
import japgolly.scalajs.react.{Callback, React}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.VdomElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.07.17 23:03
  * Description: DI-модуль линковки самого верхнего уровня sc3.
  * Все аргументы-зависимости объявлены и линкуются внутри тела модуля.
  */
object Sc3Module { outer =>

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

  lazy val sc3Circuit: Sc3Circuit = wire[Sc3Circuit]


  // Костыли для js-роутера.
  // Без костылей вероятна проблема курицы и яйца в виде циклической зависимости инстансов:
  // - Шаблонам нужен routerCtl (react-контекст) для рендера ссылок и прочего.
  // - Роутеру (уже во время роутинга) нужны инстансы шаблонов для рендера интерфейса.
  //
  // Однако, цикл неявный: все инстансы нужны НЕодновременно:
  // - инстанс роутера, который дёргает шаблоны только при необходимости.
  // - Аналогично с шаблонами: дёргают роутер только после монтирования в VDOM.
  //
  // Для явной разводки доступа к инстансам,
  // используются 0-arg функции, которые скрывают за собой lazy-инстансы.
  // Костыль для инжекции ленивого доступа к инстансу ScRootR.
  def _sc3CircuitF = (routerState: MSpaRouterState) => wire[Sc3Circuit]


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
  lazy val gridCoreR = wire[GridCoreR]
  lazy val gridR   = wire[GridR]


  // search
  lazy val searchMapR = wire[SearchMapR]
  lazy val searchR = wire[SearchR]
  lazy val nfListR = wire[NfListR]
  lazy val nfRowR = wire[NfRowR]
  lazy val nodesFoundR = wire[NodesFoundR]
  lazy val geoMapOuterR = wire[GeoMapOuterR]


  // menu
  lazy val menuR = wire[MenuR]
  lazy val versionR = wire[VersionR]
  lazy val enterLkRowR = wire[EnterLkRowR]
  lazy val aboutSioR = wire[AboutSioR]
  lazy val editAdR = wire[EditAdR]
  lazy val dlAppMenuItemR = wire[DlAppMenuItemR]
  lazy val indexesRecentR = wire[IndexesRecentR]
  lazy val scNodesBtnR = wire[ScNodesBtnR]


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
  lazy val scRootR = wire[ScRootR]
  lazy val sc3Api = wire[Sc3ApiXhrImpl]
  lazy val scAppApiHttp = wire[ScAppApiHttp]


  trait ScHttpConfigImpl extends IMHttpClientConfig {
    override def httpClientConfig = () => {
      HttpClientConfig(
        csrfToken = sc3Circuit.csrfTokenRW.value.toOption,
      )
    }
  }

  // login
  object ScLoginFormModule extends LoginFormModuleBase {

    override val diConfig: LoginFormDiConfig = new LoginFormDiConfig with ScHttpConfigImpl {

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
            val act = ReGetIndex(
              setLoggedIn = OptionUtil.SomeBool.someTrue
            )
            sc3Circuit.dispatch( act )
          } else {
            // Обычный редирект - в браузере.
            DomQuick.goToLocation( rdrUrl )
          }

          // в LoginForm делать ничего не надо:
          DoNothing
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
      LoginFormModuleBase.circuit2loginCssRCtx( outer.sc3Circuit.scLoginRW.value.circuit )

  }
  def getLoginFormCircuit = () => ScLoginFormModule.loginFormCircuit

  lazy val scLoginR = {
    import ScLoginFormModule.loginFormR
    wire[ScLoginR]
  }


  /** Поддержка lk-nodes. */
  object ScNodesFormModule extends LkNodesModuleBase {
    override def getPlatformCss = outer._getPlatformCss
    override def platformComponents = outer.platformComponents
    override val diConfig: NodesDiConf = new NodesDiConf with ScHttpConfigImpl {
      override def circuitInit() = ScNodesDiaAh.scNodesCircuitInit(
        userIsLoggedIn = sc3Circuit.loggedInRO.value,
      )
      override def scRouterCtlOpt = Some( outer.sc3SpaRouter.state.routerCtl )
      override def closeForm = Some( Callback( outer.sc3Circuit.dispatch( ScNodesShowHide(false) ) ) )
      /** Ссылки в ЛК необходимо показывать только в браузере, но не в Cordova,
        * т.к. переброс из приложения в браузер не готов, и мобильная вёрстка ЛК тоже отсутствует. */
      override def showLkLinks = outer.sc3Circuit.platformRW.value.isBrowser
    }
  }
  def getNodesFormCircuit = () => ScNodesFormModule.lkNodesFormCircuit
  lazy val scNodesR = {
    import ScNodesFormModule.lkNodesFormR
    wire[ScNodesR]
  }

}
