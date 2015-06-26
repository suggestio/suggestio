package models.msc

import models.im.MImg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:28
 * Description: Аргумента для вызова рендера шаблона sc._focusedAdsTpl.
 */
trait IFocusedAdsTplArgs extends SyncRenderInfo with IAdBodyTplArgs with IColors with ILogoRenderArgs
with IHBtnRenderArgs {
  override def title = producer.meta.nameShort
  /** Является ли данная рекламная карточка размещенной сторонним продьюсером? */
  def is3rdParty: Boolean
}

// (Тут до web21:0af93c09f23a включительно был код дефолтовой реализации модели, но реализация оказалась невостребована.)

/** Реализация модели [[IFocusedAdsTplArgs]] с передачей части аргументов в контейнере [[IAdBodyTplArgs]]. */
case class FocusedAdsTplArgs2(
  adBodyTplArgs               : IAdBodyTplArgs,
  override val bgColor        : String,
  override val fgColor        : String,
  override val hBtnArgs       : IhBtnArgs,
  override val logoImgOpt     : Option[MImg],
  override val is3rdParty     : Boolean,
  override val jsStateOpt     : Option[ScJsState] = None
)
  extends IFocusedAdsTplArgs with IAdBodyTplArgsWrapper
{

  override def _underlying = adBodyTplArgs
}


/** Враппер для [[IFocusedAdsTplArgs]]. */
trait IFocusedAdsTplArgsWrapper extends IFocusedAdsTplArgs with IAdBodyTplArgsWrapper with IColorsWrapper
with ILogoImgOptWrapper with IHbtnArgsFieldWrapper {
  override def _underlying: IFocusedAdsTplArgs

  override def is3rdParty     = _underlying.is3rdParty
  override def jsStateOpt     = _underlying.jsStateOpt
  override def syncUrl(jsState: ScJsState) = _underlying.syncUrl(jsState)
  override def syncRender     = _underlying.syncRender
}
