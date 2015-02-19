package io.suggest.xadv.ext.js.runner.c

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 10:41
 * Description: Приложение runner - с него начинается исполнение runner'а.
 */
object AeRunnerApp extends js.JSApp with WsKeeper {

  /** Запуск системы происходит здесь. */
  @JSExport
  override def main(): Unit = {
    val wsUrl = dom.document.getElementById("socialApiConnection").getAttribute("value")
    startWs(wsUrl)
  }

}

