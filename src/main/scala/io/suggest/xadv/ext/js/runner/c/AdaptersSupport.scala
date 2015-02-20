package io.suggest.xadv.ext.js.runner.c

import io.suggest.xadv.ext.js.runner.m.MJsCtx
import io.suggest.xadv.ext.js.vk.c.VkAdapter
import org.scalajs.dom

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 18:41
 * Description: Контроллер поддержки адаптеров.
 */
object AdaptersSupport {

  // Доступные адаптеры. Пока такая заглушка скорее, на будущее.
  private[this] val adapters: js.Array[IAdapter] = js.Array(
    new VkAdapter
  )

  /**
   * Обработка входящего распарсенного экшена начинается здесь.
   * @param mctx Контекст, пришедший в запросе.
   * @return Фьючерс с исходящим контекстом.
   */
  def handleAction(mctx: MJsCtx): Future[MJsCtx] = {
    mctx.action match {
      case "ensureReady" => ensureReady(mctx)
    }
  }

  def findAdapter(mctx: MJsCtx): Option[IAdapter] = {
    findAdapter1(mctx.domain, adapters.length, i = 0)
  }

  @tailrec
  private def findAdapter1(domains: js.Array[String], len: Int, i: Int): Option[IAdapter] = {
    if (i < len) {
      val res = try {
        val adapter = adapters(i)
        // Проверить, подходит ли адаптер
        if (isDomainsForAdapter(adapter, domains, domains.length))
          Some(adapter)
        else
          None
      } catch {
        case ex: Throwable =>
          dom.console.error(ex.getMessage)
          None
      }
      if (res.isEmpty)
        findAdapter1(domains, len, i = i + 1)
      else
        res
    } else {
      dom.console.warn("Not found adapter for domains requested: " + domains)
      None
    }
  }

  @tailrec
  private def isDomainsForAdapter(adapter: IAdapter, domains: js.Array[String], len: Int, i: Int = 0): Boolean = {
    if (i < len) {
      if (adapter.isMyDomain(domains(i)))
        isDomainsForAdapter(adapter, domains, len, i = i + 1)
      else
        false
    } else {
      len > 0
    }
  }

  /** Сервер просит инициализировать клиент под указанные домены. */
  def ensureReady(mctx: MJsCtx): Future[MJsCtx] = {
    // Ищем подходящий адаптер для всех запрошенных доменов.
    // TODO Переписать find() без использования scala-коллекций и их API:
    val adapterOpt = findAdapter(mctx)
    adapterOpt match {
      case Some(adapter) =>
        adapter.ensureReady(mctx)
      case None =>
        Future failed new NoSuchElementException("No adapter exist for domains: " + mctx.domain.mkString(", "))
    }
  }

}
