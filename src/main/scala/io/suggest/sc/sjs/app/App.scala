package io.suggest.sc.sjs.app

import io.suggest.sc.sjs.c.NodeCtl
import io.suggest.sc.sjs.m.magent.{MAgent, MScreen}
import io.suggest.sc.sjs.util.router.srv.SrvRouter
import io.suggest.sc.sjs.v.render.direct.DirectRrr

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

    val rrr = DirectRrr
    val scrSz = rrr.getViewportSize.get
    MAgent.availableScreen = MScreen(scrSz.width, height = scrSz.height, pxRatio = 1.0)

    // Когда состояние готово, нужно передать управление в контроллеры.
    for {
      _ <- srvRouterFut
    } yield {
      NodeCtl.switchToNode(None)
    }
  }

}
