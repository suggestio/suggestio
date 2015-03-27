package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.MAskActionLightBaseT
import io.suggest.adv.ext.model.ctx.MAskActions._
import io.suggest.xadv.ext.js.runner.c.{IActionContext, AeRunnerApp, PopupChecker}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.02.15 11:43
 * Description: Модель допустимых действий, приходящих в ask-контексте в поле ctx.action
 */
object MAskActions extends MAskActionLightBaseT {

  sealed protected trait ValT extends super.ValT {
    /**
     * Запустить экшен на исполнение.
     * @param adapter Адаптер, который необходимо дёрнуть.
     * @param actx Контекст текущего экшена.
     * @return Фьючерс с новым контекстом.
     */
    def processAction(adapter: IAdapter, actx: IActionContext): Future[MJsCtx]

    /** А требуется ли адаптер для исполнения действа? */
    def adapterRequired: Boolean
  }

  sealed protected trait AdHocAction extends ValT {
    override def adapterRequired = false
    def processAction(actx: IActionContext): Future[MJsCtx]
    override def processAction(adapter: IAdapter, actx: IActionContext): Future[MJsCtx] = {
      processAction(actx)
    }
  }

  /**
   * Абстрактный экземпляр модели.
   * @param strId Строкой id экземпляра (ключ экземпляра).
   */
  protected abstract class Val(val strId: String) extends ValT

  override type T = Val


  override val Init: T = new Val(INIT) with AdHocAction {
    override def processAction(actx: IActionContext): Future[MJsCtx] = {
      AeRunnerApp.init(actx)
    }
  }

  /** Запрос инициализации клиента. */
  override val EnsureReady: T = new Val(ENSURE_READY) {
    override def processAction(adapter: IAdapter, actx: IActionContext): Future[MJsCtx] = {
      adapter.ensureReadySafe(actx)
    }
    override def adapterRequired = true
  }

  /** Запрос размещения цели. */
  override val HandleTarget: T = new Val(HANDLE_TARGET) {
    override def processAction(adapter: IAdapter, actx: IActionContext): Future[MJsCtx] = {
      adapter.handleTargetSafe(actx)
    }
    override def adapterRequired = true
  }

}
