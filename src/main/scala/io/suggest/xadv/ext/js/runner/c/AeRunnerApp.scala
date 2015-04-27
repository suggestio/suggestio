package io.suggest.xadv.ext.js.runner.c

import io.suggest.sjs.common.controller.InitController
import io.suggest.sjs.common.view.CommonPage
import io.suggest.xadv.ext.js.fb.c.FbAdapter
import io.suggest.xadv.ext.js.runner.m.ex.CustomException
import io.suggest.xadv.ext.js.runner.m.{IAdapter, MAnswerStatuses, MJsCtx}
import io.suggest.xadv.ext.js.runner.v.Page
import io.suggest.xadv.ext.js.vk.c.VkAdapter

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 10:41
 * Description: Приложение runner - с него начинается исполнение runner'а при вызове напрямую.
 */
object AeRunnerApp extends js.JSApp {

  /** Запуск системы происходит здесь. */
  @JSExport
  override def main(): Unit = {
    Runner.start()
  }
}


/** Аддон для ri-sjs-контроллера LkAdvExt, чтобы был экшен для запуска runner'а. */
trait RunnerRiCtl extends InitController {

  override def riAction(name: String): Future[_] = {
    if (name == "runner") {
      Future {
        Runner.start()
      }
    } else {
      super.riAction(name)
    }
  }
}


/** Программный фасад всей системы. */
object Runner extends WsKeeper {

  /** Запуск всей системы runner'а. С этого начинается вся работа. */
  def start(): Unit = {
    val adapters = List[IAdapter](
      new VkAdapter,
      new FbAdapter
    )
    // Нижеследующий код зависит от dom и вызывается из document.ready.
    CommonPage.onReady { () =>
      startWs(adapters)
      Page.bindPageEvents()
    }
  }


  /** Инициализация системы по запросу сервера sio. */
  def handleInitCmd(actx: IActionContext): Future[MJsCtx] = {
    try {
      if (PopupChecker.isPopupAvailable()) {
        Future successful actx.mctx0.copy(
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

