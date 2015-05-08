package models.msc

import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:28
 * Description: Параметры для вызова showcase-шаблона focusedAdsTpl.
 */

trait FocusedAdsTplArgs extends SyncRenderInfo {
  def producer    : MAdnNode
  def bgColor     : String
  def fgColor     : String
  def hBtnArgs    : IhBtnArgs
  def brArgs      : blk.RenderArgs
  def adsCount    : Int
  def startIndex  : Int
}

/** Враппер для [[FocusedAdsTplArgs]]. */
trait FocusedAdsTplArgsWrapper extends FocusedAdsTplArgs {
  def _focArgsUnderlying: FocusedAdsTplArgs

  override def startIndex     = _focArgsUnderlying.startIndex
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
