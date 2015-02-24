package io.suggest.xadv.ext.js.runner.c

import io.suggest.xadv.ext.js.runner.m.{MAdapters, MJsCtx}
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
  def handleAction(mctx: MJsCtx): Future[MJsCtx] = {
    MAdapters.findAdapter(mctx) match {
      case Some(adapter) =>
        mctx.action.processAction(adapter, mctx)
      case None =>
        Future failed new NoSuchElementException("No adapter exist for domains: " + mctx.domain.mkString(", "))
    }
  }

}
