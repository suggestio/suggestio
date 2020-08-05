package io.suggest.sc

import com.materialui.MuiTheme
import com.softwaremill.macwire._
import io.suggest.id.login.v.LoginFormCss
import io.suggest.id.login.{LoginFormCircuit, LoginFormModuleBase}
import io.suggest.sc.m.{MScReactCtx, RouteTo}
import io.suggest.sc.m.boot.MSpaRouterState
import io.suggest.sc.u.api.ScAppApiHttp
import io.suggest.sc.v._
import io.suggest.sc.v.dia.dlapp.{DlAppDiaR, DlAppMenuItemR}
import io.suggest.sc.v.dia.err.ScErrorDiaR
import io.suggest.sc.v.dia.first.WzFirstR
import io.suggest.sc.v.dia.settings.{BlueToothSettingR, BlueToothUnAvailInfoR, GeoLocSettingR, NotificationSettingsR, OnOffSettingR, ScSettingsDiaR, SettingsMenuItemR, UnsafeOffsetSettingR}
import io.suggest.sc.v.grid._
import io.suggest.sc.v.hdr._
import io.suggest.sc.v.inx._
import io.suggest.sc.v.dia.login.ScLoginR
import io.suggest.sc.v.menu._
import io.suggest.sc.v.search._
import io.suggest.sc.v.search.found.{NfListR, NfRowR, NodesFoundR}
import io.suggest.sc.v.snack.{OfflineSnackR, ScSnacksR}
import io.suggest.sc.v.styl.{ScComponents, ScThemes}
import io.suggest.spa.SioPages
import japgolly.scalajs.react.React
import japgolly.scalajs.react.React.Context
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

  /** Сборка контейнера контекста, который будет распихан по sc-шаблонам. */
  lazy val scReactCtx: React.Context[MScReactCtx] =
    React.createContext[MScReactCtx]( null )

  /** Контекст, передающий mui theme. */
  lazy val muiThemeCtx: React.Context[MuiTheme] =
    React.createContext[MuiTheme]( null )

  def sc3RouterCtlCtx: React.Context[RouterCtl[SioPages.Sc3]] =
    React.createContext( sc3SpaRouter.state.routerCtl )

  val _getPlatform = sc3Circuit.platformRW.apply _

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
  lazy val scComponents = wire[ScComponents]
  lazy val scRootR = wire[ScRootR]
  lazy val sc3Api = wire[Sc3ApiXhrImpl]
  lazy val scAppApiHttp = wire[ScAppApiHttp]


  // login
  lazy val loginFormModule = new LoginFormModuleBase {

    private def route0: SioPages.Sc3 =
      outer.sc3Circuit.currRouteRW.value getOrElse SioPages.Sc3.empty

    override lazy val loginRouterCtl = routerCtl contramap { login: SioPages.Login =>
      // пропихнуть в роутер обновлённую страницу логина.
      (SioPages.Sc3.login set Option(login))(route0)
    }

    override lazy val routerCtlCtx: Context[RouterCtl[SioPages.Login]] =
      React.Context( loginRouterCtl )

    override lazy val loginFormCssCtx: Context[LoginFormCss] =
      LoginFormModuleBase.circuit2loginCssRCtx( outer.sc3Circuit.scLoginRW.value.circuit )

  }
  def getLoginFormCircuit = () => loginFormModule.loginFormCircuit

  lazy val scLoginR = {
    import loginFormModule.loginFormR
    wire[ScLoginR]
  }

}
