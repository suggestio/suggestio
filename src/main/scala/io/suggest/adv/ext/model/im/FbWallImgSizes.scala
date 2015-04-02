package io.suggest.adv.ext.model.im

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.15 15:50
 * Description: Шаблон статической модели размеров картинок Facebook.
 */
trait FbWallImgSizesBaseT extends ServiceSizesEnumBaseT {

  // алиасы (названия) размеров.
  def FB_SZ_COMMUNITY_ALIAS = "fbc"
  def FB_SZ_USER_ALIAS      = "fbu"

  protected trait FbPostLinkValT extends ValT {
    override def width  = 1200
    override def height = 630
  }

  protected trait FbDashboardLinkValT extends ValT {
    override def width  = 487
    override def height = 255
  }

  /** Community-страницы: page, event, group. */
  def FbPostLink: T

}


/** Абстрактная реализация размеров link-картинок Facebook через scala.Enumeration. */
trait FbWallImgSizesScalaEnumT extends ServiceSizesEnumScalaT with FbWallImgSizesBaseT {
  override val FbPostLink: T  = new Val(FB_SZ_COMMUNITY_ALIAS) with FbPostLinkValT
}


/** Заготовка Light-модели. Её можно ещё допилить. */
trait FbWallImgSizeLightBaseT extends FbWallImgSizesBaseT with ServiceSizesEnumLightT {
  protected trait FbPostLinkValT extends super.FbPostLinkValT {
    override def szAlias = FB_SZ_COMMUNITY_ALIAS
  }

  override def maybeWithName(n: String): Option[T] = {
    val fbPostLink = FbPostLink
    if (fbPostLink.szAlias == n)
      Some(fbPostLink)
    else 
      super.maybeWithName(n)
  }
}

/** Абстрактная Light-реализация размером facebook. Допиливать её уже проблематично. */
trait FbWallImgSizesLightT extends FbWallImgSizeLightBaseT {

  override type T = ValT

  override val FbPostLink: T = new ValT with FbPostLinkValT

}
