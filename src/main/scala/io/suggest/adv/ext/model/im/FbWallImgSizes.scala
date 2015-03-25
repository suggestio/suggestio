package io.suggest.adv.ext.model.im

import io.suggest.model.IVeryLightEnumeration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.15 15:50
 * Description: Шаблон статической модели размеров картинок Facebook.
 */
trait FbWallImgSizesBaseT extends IVeryLightEnumeration {

  protected trait ValT extends super.ValT with ISize2di

  protected trait CommunityValT extends ValT {
    override def width  = 470
    override def height = 246
  }

  protected trait UserValT extends ValT {
    override def width  = 487
    override def height = 255
  }

  override type T <: ValT

  /** Community-страницы: page, event, group. */
  val Community: T

  /** Пользовательские страницы. У них немного другие размеры. */
  val User: T


  def maybeWithSize(sz: ISize2di): Option[T] = {
    maybeWithSize(width = sz.width,  height = sz.height)
  }
  def maybeWithSize(width: Int, height: Int): Option[T] = {
    if (width == Community.width && height == Community.height)
      Some(Community)
    else if (width == User.width && height == User.height)
      Some(User)
    else
      None
  }

}
