package io.suggest.sc

import com.materialui.{Mui, MuiTheme}
import com.softwaremill.macwire._
import diode.react.ModelProxy
import diode.Effect
import io.suggest.adt.{ScMenuLkEnterItem, ScSearchMap}
import io.suggest.geo.IDistanceUtilJs
import io.suggest.grid.{IGridRenderer, ScGridScrollUtil}
import io.suggest.grid.build.MGridRenderInfo
import io.suggest.id.login.v.session.LogOutDiaR
import io.suggest.lk.IPlatformComponentsModule
import io.suggest.lk.api.{ILkLangApi, LkLangApiHttpImpl}
import io.suggest.lk.c.CsrfTokenApi
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.log.Log
import io.suggest.nfc.INfcApi
import io.suggest.proto.http.model.{HttpClientConfig, MCsrfToken}
import io.suggest.qr.QrCodeRenderArgs
import io.suggest.react.ComponentFunctionR
import io.suggest.routes.IJsRoutes
import io.suggest.sc.model.MScRoot
import io.suggest.sc.model.search.MGeoTabS
import io.suggest.sc.util.ScGeoUtil
import io.suggest.sc.util.api.{IScStuffApi, IScUniApi, ScAppApiHttp, ScStuffApiHttp, ScUniApiHttpImpl}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.view._
import io.suggest.sc.view.dia.dlapp._
import io.suggest.sc.view.dia.err._
import io.suggest.sc.view.dia.first._
import io.suggest.sc.view.dia.settings._
import io.suggest.sc.view.grid._
import io.suggest.sc.view.hdr._
import io.suggest.sc.view.inx._
import io.suggest.sc.view.dia.login.ScLoginR
import io.suggest.sc.view.dia.nodes.ScNodesR
import io.suggest.sc.view.menu._
import io.suggest.sc.view.search._
import io.suggest.sc.view.search.found._
import io.suggest.sc.view.snack._
import io.suggest.sc.view.styl._
import io.suggest.scroll.IScrollApi
import io.suggest.spa.{DAction, DoNothing}
import japgolly.scalajs.react.React
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import scalaz.{@@, Tag}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.07.17 23:03
  * Description: DI-модуль линковки самого верхнего уровня sc3.
  * Все аргументы-зависимости объявлены и линкуются внутри тела модуля.
  */
abstract class ScCommonModule extends Log { outer =>

  import io.suggest.jd.render.JdRenderModule._
  import io.suggest.ReactCommonModule._
  import io.suggest.lk.LkCommonModule._


  def sc3Circuit: ScCommonCircuit

  /** Defaults for grid rendering, used on each grid build and may be modified by controllers. */
  val gridRenderInfo: MGridRenderInfo

  /** Partial implementation for Sc3Circuit with already defined stuff. */
  protected abstract class ScCommonCircuitA extends ScCommonCircuit {
    override def scGeoUtil = outer.scGeoUtil
    override def scrollApiOpt = outer.scrollApiOpt
    override def scGridScrollUtil = outer.scGridScrollUtil
    override def gridRenderInfo = outer.gridRenderInfo

    override def sc3UniApi: IScUniApi = {
      import ScHttpConf._
      wire[ScUniApiHttpImpl]
    }
    override def scStuffApi: IScStuffApi = {
      import ScHttpConfCsrf._
      wire[ScStuffApiHttp]
    }
    override def csrfTokenApi = {
      import ScHttpConf._
      wire[CsrfTokenApi]
    }
    override def scAppApi = {
      import ScHttpConf._
      wire[ScAppApiHttp]
    }
    override def lkLangApi: ILkLangApi = {
      import ScHttpConfCsrf._
      wire[LkLangApiHttpImpl]
    }
  }

  // React contexts
  lazy val muiThemeCtx = React.createContext[MuiTheme]( Mui.Styles.createTheme() )
  lazy val platfromCssCtx = React.createContext[PlatformCssStatic]( sc3Circuit.platformCssRO.value )
  lazy val scCssCtx = React.createContext[ScCss]( sc3Circuit.scCssRO.value )
  lazy val jsRouterOptCtx = React.createContext[Option[IJsRoutes]]( sc3Circuit.jsRouterRW.value.jsRoutesOpt )


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
  def gridRenderer: IGridRenderer
  lazy val gridR   = wire[GridR]
  lazy val locationButtonR = wire[LocationButtonR]


  // search
  def searchMapOptF: Option[ComponentFunctionR[ModelProxy[MGeoTabS]] @@ ScSearchMap]
  lazy val searchR = wire[SearchR]
  lazy val nfListR = wire[NfListR]
  lazy val nfRowsR = wire[NfRowsR]
  lazy val nodesFoundR = wire[NodesFoundR]
  lazy val geoMapOuterR = wire[GeoMapOuterR]


  // menu
  lazy val menuR = wire[MenuR]
  lazy val menuItemR = wire[MenuItemR]
  lazy val versionR = wire[VersionR]
  def enterLkRowROpt: Option[ComponentFunctionR[ModelProxy[MScRoot]] @@ ScMenuLkEnterItem]
  lazy val aboutSioR = wire[AboutSioR]
  lazy val editAdR = wire[EditAdR]
  lazy val dlAppMenuItemR = wire[DlAppMenuItemR]
  lazy val indexesRecentR = wire[IndexesRecentR]
  lazy val scNodesMenuItemR = wire[ScNodesMenuItemR]
  lazy val logOutR = wire[LogOutR]


  // dia
  lazy val wzFirstR = wire[WzFirstR]
  /** QR-code rendering component/function. */
  def qrCodeRenderF: Option[ModelProxy[QrCodeRenderArgs] => VdomNode]
  lazy val dlAppDiaR = wire[DlAppDiaR]

  // snack
  lazy val scErrorDiaR = wire[ScErrorDiaR]
  lazy val indexSwitchAskR = wire[IndexSwitchAskR]
  def scSnacksROpt: Option[ScSnacksR]
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
  lazy val langSettingR = wire[LangSettingR]

  // sc3
  lazy val scThemes = wire[ScThemes]
  lazy val scRootR = wire[ScRootR]
  def scRootRendered: VdomElement =
    sc3Circuit.wrap( identity(_) )( scRootR.component.apply )

  /** Interface for abstraction of HttpClientConfig generator. */
  protected trait IScHttpConf {
    def mkHttpClientConfig(csrf: Option[MCsrfToken]): HttpClientConfig
  }
  /** HTTP Client config generator abstraction. */
  protected def _httpConfMaker: IScHttpConf

  object ScHttpConf {
    /** HTTP-config maker function.
      * ** WITHOUT CSRF support! **
      */
    val mkRootHttpClientConfigF = { () =>
      _httpConfMaker.mkHttpClientConfig( None )
    }
  }

  /** Активация поддержки CSRF для всех запросов. */
  object ScHttpConfCsrf {
    val httpClientConfig = () => {
      val csrf2 = sc3Circuit.csrfTokenRW.value.toOption
      _httpConfMaker.mkHttpClientConfig( csrf2 )
    }
  }


  protected trait ScPlatformComponents extends IPlatformComponentsModule {
    override def getPlatformCss = outer._getPlatformCss
    override def platformComponents = outer.platformComponents
  }

  def logOutDiaROpt: Option[LogOutDiaR]

  def scLoginROpt: Option[ScLoginR]

  /** Make instance for NFC API, used by ShowCase. */
  def nfcApiOpt: Option[INfcApi]


  def scNodesROpt: Option[ScNodesR]


  protected[this] def _toSc3CircuitFx(action: DAction): Effect = {
    Effect.action {
      sc3Circuit.dispatch( action )
      DoNothing
    }
  }

  def distanceUtilJsOpt: Option[IDistanceUtilJs]
  def scGeoUtil = wire[ScGeoUtil]

  def scrollApiOpt: Option[IScrollApi]
  def scGridScrollUtil = wire[ScGridScrollUtil]

}
