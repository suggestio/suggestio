package io.suggest.sc.sjs.app

import io.suggest.sc.sjs.c.NodeCtl
import io.suggest.sc.sjs.m.magent.vsz.ViewportSz
import io.suggest.sc.sjs.m.magent.{MAgent, MScreen}
import io.suggest.sc.sjs.util.router.srv.SrvRouter
import io.suggest.sjs.common.util.SjsLogger

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.15 15:13
 * Description: JSApp, который будет экспортироваться наружу для возможности запуска выдачи.
 */
object App extends JSApp with SjsLogger {

  @JSExport
  override def main(): Unit = {
    // Собрать исходное состояние системы
    val srvRouterFut = SrvRouter.getRouter

    val scrSz = ViewportSz.getViewportSize.get
    MAgent.availableScreen = MScreen(scrSz.width, height = scrSz.height, pxRatio = 1.0)

    // Когда состояние готово, нужно передать управление в контроллеры.
    val fut = for {
      _ <- srvRouterFut
    } yield {
      NodeCtl.switchToNode(None, isFirstRun = true)
    }

    fut onFailure { case ex: Throwable =>
      error("Init failed", ex)
    }
  }

}
