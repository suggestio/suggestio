package io.suggest.xadv.ext.js.runner.c

import io.suggest.xadv.ext.js.runner.m.ex.CustomException
import io.suggest.xadv.ext.js.runner.m.{MAnswerStatuses, MJsCtx}
import org.scalajs.dom

import scala.concurrent.Future
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


  /** Инициализация системы по запросу сервера sio. */
  def init(mctx0: MJsCtx): Future[MJsCtx] = {
    try {
      if (PopupChecker.isPopupAvailable()) {
        Future successful mctx0.copy(
          status = Some(MAnswerStatuses.Success)
        )

      } else {
        Future failed CustomException("e.adv.ext.your.browser.block.popups")
      }
    } catch {
      case ex: Throwable =>
        Future failed CustomException("e.adv.ext.api.init", cause = Some(ex))
    }
  }

}

