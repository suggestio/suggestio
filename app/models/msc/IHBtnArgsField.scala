package models.msc

import io.suggest.primo.IUnderlying

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.06.15 17:33
 * Description: Интерфейс поля hBtnArgs в разных моделях контейнеров аргументов.
 */
trait IHBtnArgsField {

  /** Параметры рендера header-кнопок. */
  def hBtnArgs: IhBtnArgs

}


/** Враппер для реализаций модели [[IHBtnArgsField]]. */
trait IHbtnArgsFieldWrapper extends IHBtnArgsField with IUnderlying {
  override def _underlying: IHBtnArgsField
  override def hBtnArgs = _underlying.hBtnArgs
}
