package io.suggest.xadv.ext.js.runner.c

import io.suggest.init.routed.InitRouter
import io.suggest.sjs.common.model.wsproto.MAnswerStatuses
import io.suggest.sjs.common.view.CommonPage
import io.suggest.xadv.ext.js.fb.c.FbAdapter
import io.suggest.xadv.ext.js.runner.m.ex.CustomException
import io.suggest.xadv.ext.js.runner.m.{IAdapter, MJsCtx}
import io.suggest.xadv.ext.js.runner.v.Page
import io.suggest.xadv.ext.js.vk.c.VkAdapter
import japgolly.univeq._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 10:41
 * Description: Приложение runner - с него начинается исполнение runner'а при вызове напрямую.
 */

/** Аддон для ri-sjs-контроллера LkAdvExt, чтобы был экшен для запуска runner'а. */
trait AdvExtRunnerInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (itg ==* MJsInitTargets.AdvExtRunner) {
      Runner.start()
    } else {
      super.routeInitTarget(itg)
    }
  }

}


// TODO Сделать это через class (нестатическим). Для этого надо вынести handleInitCmd() куда-то ещё.

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

