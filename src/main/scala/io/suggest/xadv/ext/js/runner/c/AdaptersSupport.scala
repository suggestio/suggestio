package io.suggest.xadv.ext.js.runner.c

import io.suggest.xadv.ext.js.runner.m.MJsCtx
import io.suggest.xadv.ext.js.vk.c.VkAdapter

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 18:41
 * Description: Контроллер поддержки адаптеров.
 */
object AdaptersSupport {

  // Доступные адаптеры.
  private[this] val adapters: List[IAdapter] = List(
    new VkAdapter
  )

  def handleAction(mctx: MJsCtx): Future[MJsCtx] = {
    mctx.action match {
      case "ensureReady" => ensureReady(mctx)
    }
  }

  /** Сервер просит инициализировать клиент под указанные домены. */
  def ensureReady(mctx: MJsCtx): Future[MJsCtx] = {
    // Ищем подходящий адаптер для всех запрошенных доменов.
    val adapterOpt = adapters.find { adapter =>
      mctx.domain.forall { domain =>
        adapter.isMyDomain(domain)
      }
    }
    adapterOpt match {
      case Some(adapter) =>
        adapter.ensureReady(mctx)
      case None =>
        Future failed new NoSuchElementException("No adapter exist for domains: " + mctx.domain.mkString(", "))
    }
  }

}
