package io.suggest.sc.sjs.app

import io.suggest.sc.sjs.c.ScFsm
import io.suggest.sjs.common.util.SjsLogger

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.15 15:13
 * Description: JSApp, который будет экспортироваться наружу для возможности запуска выдачи.
 */
object App extends JSApp with SjsLogger {

  @JSExport
  override def main(): Unit = {
    ScFsm.firstStart()
  }

}
