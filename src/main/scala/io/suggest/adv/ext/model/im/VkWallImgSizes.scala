package io.suggest.adv.ext.model.im


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.15 17:50
 * Description: Модели размеров настенных картинок для vk-сервиса.
 */
trait VkWallImgSizesBaseT extends ServiceSizesEnumBaseT {

  def VK_WALL_IMG_SIZE_DFLT_ALIAS = "vk1"

  protected trait VkWallDfltValT extends ValT {
    override def height = 700
    override def width  = 1100
  }

  /** Community-страницы: page, event, group. */
  val VkWallDflt: T

}


trait VkWallImgSizesScalaEnumT extends ServiceSizesEnumScalaT with VkWallImgSizesBaseT {
  override val VkWallDflt: T = new Val(VK_WALL_IMG_SIZE_DFLT_ALIAS) with VkWallDfltValT
}


/** Заготовка Light-модели. Её можно ещё допилить в реализациях. */
trait VkWallImgSizeLightBaseT extends VkWallImgSizesBaseT with ServiceSizesEnumLightT {
  protected trait VkWallDfltValT extends super.VkWallDfltValT {
    override def szAlias = VK_WALL_IMG_SIZE_DFLT_ALIAS
  }

  override def maybeWithName(n: String): Option[T] = {
    if (VkWallDflt.szAlias == n)
      Some(VkWallDflt)
    else
      super.maybeWithName(n)
  }
}


/** Абстрактная Light-реализация размером facebook. Допиливать её уже проблематично. */
trait VkWallImgSizesLightT extends VkWallImgSizeLightBaseT {
  override type T = ValT
  override val VkWallDflt: T = new ValT with VkWallDfltValT
}
