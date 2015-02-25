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
  private[this] val adapters = Seq[IAdapter](
    new VkAdapter
  )


  /** Поиск подходящего адаптера-исполнителя под контекст запроса. */
  def findAdapter(mctx: MJsCtx): Option[IAdapter] = {
    adapters.find { adapter =>
      adapter.isMyDomains(mctx.domains)
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

  /** Запуск обработки одной цели. */
  def handleTarget(mctx0: MJsCtx): Future[MJsCtx]

  /** Враппер над isMyDomain(), позволяющий быстро и решительно обойти массив доменов. */
  def isMyDomains(domains: Seq[String]): Boolean = {
    domains forall isMyDomain
  }

}
