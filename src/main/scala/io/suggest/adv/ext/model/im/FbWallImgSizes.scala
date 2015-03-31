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

  protected trait FbCommunityValT extends ValT {
    override def width  = 470
    override def height = 246
  }

  protected trait FbUserValT extends ValT {
    override def width  = 487
    override def height = 255
  }

  /** Community-страницы: page, event, group. */
  val FbCommunityLink: T

  /** Пользовательские страницы. У них немного другие размеры. */
  val FbUserLink: T

}


/** Абстрактная реализация размеров link-картинок Facebook через [[scala.Enumeration]]. */
trait FbWallImgSizesScalaEnumT extends ServiceSizesEnumScalaT with FbWallImgSizesBaseT {
  override val FbCommunityLink: T = new Val(FB_SZ_COMMUNITY_ALIAS) with FbCommunityValT
  override val FbUserLink: T      = new Val(FB_SZ_USER_ALIAS) with FbUserValT
}


/** Заготовка Light-модели. Её можно ещё допилить. */
trait FbWallImgSizeLightBaseT extends FbWallImgSizesBaseT with ServiceSizesEnumLightT {
  protected trait FbCommunityValT extends super.FbCommunityValT {
    override def szAlias = FB_SZ_COMMUNITY_ALIAS
  }
  protected trait FbUserValT extends super.FbUserValT {
    override def szAlias = FB_SZ_USER_ALIAS
  }

  override def maybeWithName(n: String): Option[T] = {
    if (FbCommunityLink.szAlias == n)
      Some(FbCommunityLink)
    else if (FbUserLink.szAlias == n)
      Some(FbUserLink)
    else
      super.maybeWithName(n)
  }
}

/** Абстрактная Light-реализация размером facebook. Допиливать её уже проблематично. */
trait FbWallImgSizesLightT extends FbWallImgSizeLightBaseT {

  override type T = ValT

  override val FbCommunityLink: T = new ValT with FbCommunityValT
  override val FbUserLink: T = new ValT with FbUserValT

}
