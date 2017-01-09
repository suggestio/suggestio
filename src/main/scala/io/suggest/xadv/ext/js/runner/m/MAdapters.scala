package io.suggest.xadv.ext.js.runner.m

import io.suggest.xadv.ext.js.runner.c.IActionContext

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.15 13:18
 * Description: Модель допустимых адаптеров и работа с ней.
 * Данные о текущих адаптерах хранятся в модели [[MAppState]].
 */
object MAdapters {

  /** Поиск подходящего адаптера-исполнителя под контекст запроса. */
  def findAdapterFor(mctx: MJsCtx, adapters: Seq[IAdapter]): Option[IAdapter] = {
    val ds = mctx.domains
    if (ds.nonEmpty) {
      adapters.find { adapter =>
        adapter isMyDomains ds
      }
    } else {
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
  def ensureReady(implicit actx: IActionContext): Future[MJsCtxT]

  /** Враппер для простого перехвата синхронных исключений в асинхронных экшенах. */
  protected def safe[T](f: => Future[T]): Future[T] = {
    try {
      f
    } catch {
      case ex: Throwable =>
        Future failed ex
    }
  }

  def ensureReadySafe(implicit actx: IActionContext): Future[MJsCtxT] = {
    safe {
      ensureReady
    }
  }


  /** Враппер над isMyDomain(), позволяющий быстро и решительно обойти массив доменов. */
  def isMyDomains(domains: Seq[String]): Boolean = {
    domains forall isMyDomain
  }


  /** Запуск обработки одной цели. */
  def handleTarget(implicit actx: IActionContext): Future[MJsCtxT]

  /**
   * Враппер над handleTarget(), но отрабатывающий возможные экзешены до наступления асинхронной части.
   * @param actx Исходный контекст.
   * @return Асинхронный результат с новым контекстом.
   */
  def handleTargetSafe(implicit actx: IActionContext): Future[MJsCtxT] = {
    safe {
      handleTarget
    }
  }

}
