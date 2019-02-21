package io.suggest.xadv.ext.js.runner.c

import io.suggest.adv.ext.model.ctx.{MAskAction, MAskActions}
import io.suggest.common.ws.proto.MAnswerStatuses
import io.suggest.xadv.ext.js.runner.m._
import org.scalajs.dom

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
  def handleAction(mctx: MJsCtx, appState: MAppState): Future[MJsCtxT] = {
    // Сборка контекста текущего экшена.
    val actx = ActionContextImpl(
      app = appState.appContext,
      mctx0 = mctx
    )
    if ( adapterRequired(mctx.action) ) {
      MAdapters.findAdapterFor(mctx, appState.adapters) match {
        case Some(adapter) =>
          processAction(mctx.action, adapter, actx )
        case None =>
          Future failed new NoSuchElementException("No adapter exist for domains: " + mctx.domains.mkString(", "))
      }

    } else {
      // Не требуется адаптера. Значит передаем null вместо адаптера.
      processAction(mctx.action, adapter = null, actx )
    }
  }

  def adapterRequired( aa: MAskAction ): Boolean = {
    aa match {
      case MAskActions.Init             => false
      case MAskActions.EnsureReady      => true
      case MAskActions.HandleTarget     => true
      case MAskActions.StorageGet       => false
      case MAskActions.StorageSet       => false
    }
  }

  def processAction(aa: MAskAction, adapter: IAdapter, actx: IActionContext): Future[MJsCtxT] = {
    aa match {
      case MAskActions.Init =>
        Runner.handleInitCmd(actx)

      case MAskActions.EnsureReady =>
        adapter.ensureReadySafe(actx)

      case MAskActions.HandleTarget =>
        adapter.handleTargetSafe(actx)

      case MAskActions.StorageGet =>
        val msk = MStorageKvCtx.fromJson( actx.mctx0.custom.get )
        val stor = dom.window.localStorage
        val msk1 = msk.copy(
          value = Option( stor.getItem(msk.key) )
        )
        val mctx1 = actx.mctx0.copy(
          status = Some(MAnswerStatuses.Success),
          custom = Some(msk1.toJson)
        )
        Future successful mctx1

      case MAskActions.StorageSet =>
        val msk = MStorageKvCtx.fromJson( actx.mctx0.custom.get )
        val stor = dom.window.localStorage
        msk.value match {
          case Some(v)  => stor.setItem(msk.key, v)
          case None     => stor.removeItem(msk.key)
        }
        // Отвечать назад ничего не надо.
        Future successful null
    }
  }

}
