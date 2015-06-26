package models.msc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:28
 * Description: Параметры для вызова showcase-шаблона focusedAdsTpl.
 */


/** Аргументы для рендера focused-карточки с полным обрамлением. */
trait IFocusedAdsTplArgs extends SyncRenderInfo with IAdBodyTplArgs with IColors {
  def hBtnArgs          : IhBtnArgs
}

// (Тут до web21:0af93c09f23a включительно был код дефолтовой реализации модели, но реализация оказалась невостребована.)

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
trait IFocusedAdsTplArgsWrapper extends IFocusedAdsTplArgs with IAdBodyTplArgsWrapper with IColorsWrapper {
  override def _underlying: IFocusedAdsTplArgs

  override def hBtnArgs       = _underlying.hBtnArgs
  override def jsStateOpt     = _underlying.jsStateOpt
  override def syncUrl(jsState: ScJsState) = _underlying.syncUrl(jsState)
  override def syncRender     = _underlying.syncRender
}
