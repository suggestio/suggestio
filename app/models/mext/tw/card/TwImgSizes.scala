package models.mext.tw.card

import io.suggest.adv.ext.model.im.INamedSize2di
import io.suggest.model.EnumMaybeWithName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.04.15 18:42
 * Description: Размеры картинок для карточек твиттера.
 */
object TwImgSizes extends EnumMaybeWithName {

  /**
   * Абстрактный экземпляр модели.
   * @param szAlias Внутренний id размера.
   */
  protected abstract sealed class Val(val szAlias: String)
    extends super.Val(szAlias)
    with INamedSize2di

  override type T = Val

  /** Размер картинки для фотокарточек. */
  val Photo: T = new Val("p") {
    override def width  = 1024
    override def height = 512
  }

}
