package io.suggest.xadv.ext.js.runner.c

import io.suggest.xadv.ext.js.fb.c.FbAdapter
import io.suggest.xadv.ext.js.runner.m.ex.CustomException
import io.suggest.xadv.ext.js.runner.m.{IAdapter, MAnswerStatuses, MJsCtx}
import io.suggest.xadv.ext.js.runner.v.Page
import io.suggest.xadv.ext.js.vk.c.VkAdapter

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
    val adapters = List[IAdapter](
      new VkAdapter,
      new FbAdapter
    )
    // Нижеследующий код зависит от dom и вызывается из document.ready.
    Page.onReady { () =>
      startWs(adapters)
      Page.bindPageEvents()
    }
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

