package io.suggest.xadv.ext.js.runner.m

import io.suggest.xadv.ext.js.vk.c.VkAdapter
import org.scalajs.dom

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.15 13:18
 * Description: Модель допустимых адаптеров. Вынос кода из c.AdaptersSupport.
 */
object MAdapters {

  /** Доступные адаптеры. Пока такая заглушка скорее, на будущее. */
  private[this] val adapters = js.Array[IAdapter](
    new VkAdapter
  )


  /** Поиск подходящего адаптера-исполнителя под контекст запроса. */
  def findAdapter(mctx: MJsCtx): Option[IAdapter] = {
    findAdapter1(mctx.domain, adapters.length, i = 0)
  }

  @tailrec
  private def findAdapter1(domains: js.Array[String], len: Int, i: Int): Option[IAdapter] = {
    if (i < len) {
      val res = try {
        val adapter = adapters(i)
        // Проверить, подходит ли адаптер
        if (adapter.isMyDomains(domains))
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

}


/** Интерфейс динамического клиента. Здесь это интерфейс экземпляра модели [[MAdapters]].
  * Но в рамках конкретной реализации это контроллер. */
trait IAdapter {

  /** Относится ли указанный домен к текущему клиенту? */
  def isMyDomain(domain: String): Boolean

  /** Запуск инициализации клиента. Добавляется необходимый js на страницу,  */
  def ensureReady(mctx0: MJsCtx): Future[MJsCtx]

  /** Враппер над isMyDomain(), позволяющий быстро и решительно обойти массив доменов. */
  def isMyDomains(domains: js.Array[String]): Boolean = {
    _isMyDomains(domains, len = domains.length, i = 0)
  }

  @tailrec
  private def _isMyDomains(domains: js.Array[String], len: Int, i: Int = 0): Boolean = {
    if (i < len) {
      if (isMyDomain(domains(i)))
        _isMyDomains(domains, len, i = i + 1)
      else
        false
    } else {
      len > 0
    }
  }

}
