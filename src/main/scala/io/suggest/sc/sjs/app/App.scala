package io.suggest.sc.sjs.app

import io.suggest.sc.sjs.c.NodeCtl
import io.suggest.sc.sjs.m.MAppState
import io.suggest.sc.sjs.m.magent.{IAppStateAgent, MScreen}
import io.suggest.sc.sjs.m.mgeo.MAppStateLocation
import io.suggest.sc.sjs.m.msrv.MAppStateSrv
import io.suggest.sc.sjs.util.router.srv.SrvRouter
import io.suggest.sc.sjs.v.render.direct.DirectRrr
import io.suggest.sjs.common.view.SafeDocument
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.15 15:13
 * Description: JSApp, который будет экспортироваться наружу для возможности запуска выдачи.
 */
object App extends JSApp {
  @JSExport
  override def main(): Unit = {
    // Собрать исходное состояние системы
    val srvRouterFut = SrvRouter.getRouter
    val rrr = new DirectRrr
    val scrSz = rrr.getViewportSize.get

    val agentState = new IAppStateAgent {
      override def availableScreen = MScreen(scrSz.width, height = scrSz.height, pxRatio = 1.0)
    }
    val locState = new MAppStateLocation()

    val appStateFut = for {
      router <- srvRouterFut
    } yield {
      val srvState = new MAppStateSrv(
        routes = router
      )
      val d = dom.document
      MAppState(
        srv       = srvState,
        location  = locState,
        agent     = agentState,
        safeDoc   = SafeDocument(d),
        renderer  = rrr
      )
    }

    // Когда состояние готово, нужно передать управление контроллерам.
    for {
      appState <- appStateFut
    } yield {
      NodeCtl.switchToNode(None)(appState)
    }
  }
}
