package models.msc

import models.{MAdnNode, blk}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:28
 * Description: Параметры для вызова showcase-шаблона focusedAdsTpl.
 */


/** Аргументы для рендера focused-карточки с полным обрамлением. */
trait IFocusedAdsTplArgs extends SyncRenderInfo with IAdBodyTplArgs {
  def bgColor           : String
  def fgColor           : String
  def hBtnArgs          : IhBtnArgs
}


/** Дефолтовая реализация [[IFocusedAdsTplArgs]]. */
case class FocusedAdsTplArgs(
  override val producer : MAdnNode,
  override val bgColor  : String,
  override val fgColor  : String,
  override val hBtnArgs : IhBtnArgs,
  override val brArgs   : blk.RenderArgs,
  override val adsCount : Int,
  override val index    : Int,
  override val jsStateOpt: Option[ScJsState] = None
)
  extends IFocusedAdsTplArgs


/** Реализация модели [[IFocusedAdsTplArgs]] с передачей части аргументов в контейнере [[IAdBodyTplArgs]]. */
case class FocusedAdsTplArgs2(
  adBodyTplArgs               : IAdBodyTplArgs,
  override val bgColor        : String,
  override val fgColor        : String,
  override val hBtnArgs       : IhBtnArgs,
  override val jsStateOpt     : Option[ScJsState] = None
)
  extends IFocusedAdsTplArgs with IAdBodyTplArgsWrapper {

  override def _underlying = adBodyTplArgs

}


/** Враппер для [[IFocusedAdsTplArgs]]. */
trait IFocusedAdsTplArgsWrapper extends IFocusedAdsTplArgs {
  def _focArgsUnderlying: IFocusedAdsTplArgs

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
