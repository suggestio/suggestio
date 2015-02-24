package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.MAskActionLightBaseT
import io.suggest.adv.ext.model.ctx.MAskActions._
import io.suggest.xadv.ext.js.runner.c.IAdapter

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.15 11:43
 * Description: Модель допустимых действий, приходящих в ask-контексте в поле ctx.action
 */
object MAskActions extends MAskActionLightBaseT {

  protected abstract class Val(val strId: String) extends ValT {
    /**
     * Запустить экшен на исполнение.
     * @param adapter Адаптер, который необходимо дёрнуть.
     * @param mctx Текущий контекст.
     * @return Фьючерс с новым контекстом.
     */
    def processAction(adapter: IAdapter, mctx: MJsCtx): Future[MJsCtx]
  }

  override type T = Val

  override val EnsureReady: T = new Val(ENSURE_READY) {
    override def processAction(adapter: IAdapter, mctx: MJsCtx): Future[MJsCtx] = {
      adapter.ensureReady(mctx)
    }
  }

  override val HandleTarget: T = new Val(HANDLE_TARGET) {
    override def processAction(adapter: IAdapter, mctx: MJsCtx): Future[MJsCtx] = ???
  }

}
