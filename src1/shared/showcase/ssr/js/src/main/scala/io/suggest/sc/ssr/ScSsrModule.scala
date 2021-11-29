package io.suggest.sc.ssr

import com.softwaremill.macwire._
import diode.data.Pot
import io.suggest.dev.{MPlatformS, MScreen}
import io.suggest.grid.PlainGridRenderer
import io.suggest.grid.build.MGridRenderInfo
import io.suggest.proto.http.model.{HttpClientConfig, MCsrfToken}
import io.suggest.routes.routes
import io.suggest.sc.model.MScRoot
import io.suggest.sc.{ScCommonCircuit, ScCommonModule}
import io.suggest.spa.CircuitUtil

/** Linking for SSR-oriented showcase. */
object ScSsrModule extends ScCommonModule { outer =>

  override def distanceUtilJsOpt = None
  override def nfcApiOpt = None
  override def searchMapOptF = None
  override def qrCodeRenderF = None
  override def logOutDiaROpt = None
  override def scNodesROpt = None
  override def scLoginROpt = None
  override def enterLkRowROpt = None
  override def scrollApiOpt = None
  override def scSnacksROpt = None

  override val gridRenderInfo = MGridRenderInfo.forSsr
  override def gridRenderer = wire[PlainGridRenderer]

  /** HTTP Client config generator abstraction. */
  override protected def _httpConfMaker: IScHttpConf = {
    new IScHttpConf {
      override def mkHttpClientConfig(csrf: Option[MCsrfToken]): HttpClientConfig = {
        HttpClientConfig(
          csrfToken = csrf,
        )
      }
    }
  }


  override lazy val sc3Circuit: ScCommonCircuit = new ScCommonCircuitA {

    override protected def initialModel: MScRoot = {
      new InitialModel {
        override val scInit = scInitDefault
        override def scConf = scInit.conf
        override def generation = 0L
        override def _mscreen = MScreen.defaulted(
          height = 2000,
        )
        override def mplatform = MPlatformS(
          isUsingNow  = true,
          isReadyPot  = Pot.empty.ready(true),
          isCordova   = false,
          osFamily    = None,
        )
        // jsRoutes must already be here in global variable.
        override def _jsRoutesPot = super._jsRoutesPot.ready( routes )
      }
        .scRoot
    }

    override def geoLocApis() = LazyList.empty
    override def beaconApis() = LazyList.empty

    val _blackHoleAh: HandlerFunction = CircuitUtil.blackholeActionHandler[MScRoot]

    override def scLoginDiaAh = _blackHoleAh
    override def scNodesDiaAh = _blackHoleAh
    override def leafletGeoLocAhOpt = None
    override def scHwButtonsAh = _blackHoleAh
    override def rcvrMarkersInitAh = _blackHoleAh
    override def mapAhs = _blackHoleAh
    override def beaconerAh = _blackHoleAh
    override def daemonSleepTimerAh = _blackHoleAh
    override def onLineAh = _blackHoleAh
    override def logOutAh = _blackHoleAh
    override def scRoutingAh = _blackHoleAh
    override def welcomeAh = _blackHoleAh

    override def isNeedBootPerms() = false
    override def isNeedGeoLocOnResume() = false
  }

}
