package io.suggest.xadv.ext.js.runner.c

import io.suggest.xadv.ext.js.runner.m.{IAdapter, MAdapters, MJsCtx}
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 18:41
 * Description: Контроллер поддержки адаптеров.
 */
object AdaptersSupport {

  /**
   * Обработка входящего распарсенного экшена начинается здесь.
   * @param mctx Контекст, пришедший в запросе.
   * @return Фьючерс с исходящим контекстом.
   */
  def handleAction(mctx: MJsCtx, adapters: Seq[IAdapter]): Future[MJsCtx] = {
    if (mctx.action.adapterRequired) {
      MAdapters.findAdapterFor(mctx, adapters) match {
        case Some(adapter) =>
          mctx.action.processAction(adapter, mctx)
        case None =>
          Future failed new NoSuchElementException("No adapter exist for domains: " + mctx.domains.mkString(", "))
      }

    } else {
      // Не требуется адаптера. Значит передаем null вместо адаптера.
      mctx.action.processAction(adapter = null, mctx)
    }
  }

}
