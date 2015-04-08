package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.MAskActionLightBaseT
import io.suggest.adv.ext.model.ctx.MAskActions._
import io.suggest.xadv.ext.js.runner.c.{IActionContext, AeRunnerApp, PopupChecker}
import org.scalajs.dom

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
    def processAction(adapter: IAdapter, actx: IActionContext): Future[MJsCtxT]

    /** А требуется ли адаптер для исполнения действа? */
    def adapterRequired: Boolean = true
  }

  sealed protected trait AdHocAction extends ValT {
    override def adapterRequired = false
    def processAction(actx: IActionContext): Future[MJsCtxT]
    override def processAction(adapter: IAdapter, actx: IActionContext): Future[MJsCtxT] = {
      processAction(actx)
    }
  }

  /**
   * Абстрактный экземпляр модели.
   * @param strId Строкой id экземпляра (ключ экземпляра).
   */
  protected abstract class Val(val strId: String) extends ValT

  override type T = Val


  /** Глобальная инициализация. */
  override val Init: T = new Val(INIT) with AdHocAction {
    override def processAction(actx: IActionContext): Future[MJsCtxT] = {
      AeRunnerApp.init(actx)
    }
  }

  /** Запрос инициализации клиента. */
  override val EnsureReady: T = new Val(ENSURE_READY) {
    override def processAction(adapter: IAdapter, actx: IActionContext): Future[MJsCtxT] = {
      adapter.ensureReadySafe(actx)
    }
  }

  /** Запрос размещения цели. */
  override val HandleTarget: T = new Val(HANDLE_TARGET) {
    override def processAction(adapter: IAdapter, actx: IActionContext): Future[MJsCtxT] = {
      adapter.handleTargetSafe(actx)
    }
  }

  /** чтение из хранилища. */
  override val StorageGet: T = new Val(STORAGE_GET) with AdHocAction {
    override def processAction(actx: IActionContext): Future[MJsCtxT] = {
      val msk = MStorageKvCtx.fromJson( actx.mctx0.custom.get )
      val stor = dom.localStorage
      val msk1 = msk.copy(
        value = Option( stor.getItem(msk.key) )
      )
      val mctx1 = actx.mctx0.copy(
        status = Some(MAnswerStatuses.Success),
        custom = Some(msk1.toJson)
      )
      Future successful mctx1
    }
  }

  /** Запись/стирание из хранилища. */
  override val StorageSet: T = new Val(STORAGE_SET) with AdHocAction {
    override def processAction(actx: IActionContext): Future[MJsCtxT] = {
      val msk = MStorageKvCtx.fromJson( actx.mctx0.custom.get )
      val stor = dom.localStorage
      msk.value match {
        case Some(v)  => stor.setItem(msk.key, v)
        case None     => stor.removeItem(msk.key)
      }
      // Отвечать назад ничего не надо.
      Future successful null
    }
  }

}
