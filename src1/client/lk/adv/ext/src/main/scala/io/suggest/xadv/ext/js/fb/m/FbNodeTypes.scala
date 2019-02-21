package io.suggest.xadv.ext.js.fb.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.adv.ext.model.im.{FbImgSize, FbImgSizes}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.15 16:30
 * Description: Модель типов узлов фейсбука: люди, страницы, группы, события и т.д.
 */
object FbNodeTypes extends StringEnum[FbNodeType] {

  /** Юзер, т.е. человек. */
  case object User extends FbNodeType("user")

  /** Страница, т.е. некий "сайт". */
  case object Page extends FbNodeType("page") {
    override def needPageToken = true
    override def publishPerms = Seq(FbPermissions.ManagePages, FbPermissions.PublishPages)
    override def postWithPrivacy = None
  }

  /** Группа юзеров. */
  case object Group extends FbNodeType("group")

  /** Календарное событие. Редкость. */
  case object Event extends FbNodeType("event")


  override def values = findValues

}


sealed abstract class FbNodeType(override val value: String) extends StringEnumEntry {
  @inline final def mdType = value

  def needPageToken: Boolean = false
  def wallImgSz: FbImgSize = FbImgSizes.FbPostLink
  override final def toString = value

  /** Выставлены дефолтовые publishPerms(). Чисто для укорачивания кода и оптимизации. */
  def publishPerms: Seq[FbPermission] =
    FbPermissions.PublishActions :: Nil

  def postWithPrivacy: Option[FbPrivacy] = Some( FbPrivacy() )
}
