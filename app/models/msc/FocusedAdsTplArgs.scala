package models.msc

import models.blk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:28
 * Description: Параметры для вызова showcase-шаблона focusedAdsTpl.
 */


/** Аргументы для рендера focused-карточки с полным обрамлением. */
trait FocusedAdsTplArgs extends SyncRenderInfo with IAdBodyTplArgs {
  def bgColor           : String
  def fgColor           : String
  def hBtnArgs          : IhBtnArgs
  override def brArgs   : blk.RenderArgs
}

/** Враппер для [[FocusedAdsTplArgs]]. */
trait FocusedAdsTplArgsWrapper extends FocusedAdsTplArgs {
  def _focArgsUnderlying: FocusedAdsTplArgs

  override def index          = _focArgsUnderlying.index
  override def producer       = _focArgsUnderlying.producer
  override def brArgs         = _focArgsUnderlying.brArgs
  override def bgColor        = _focArgsUnderlying.bgColor
  override def fgColor        = _focArgsUnderlying.fgColor
  override def hBtnArgs       = _focArgsUnderlying.hBtnArgs
  override def adsCount       = _focArgsUnderlying.adsCount
  override def jsStateOpt     = _focArgsUnderlying.jsStateOpt
  override def syncUrl(jsState: ScJsState) = _focArgsUnderlying.syncUrl(jsState)
  override def syncRender     = _focArgsUnderlying.syncRender
}
