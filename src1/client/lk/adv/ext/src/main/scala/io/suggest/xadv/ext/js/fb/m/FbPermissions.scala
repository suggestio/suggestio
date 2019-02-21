package io.suggest.xadv.ext.js.fb.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import io.suggest.sjs.common.model.{IToJson, MaybeFromJsonT}
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.{Any, Array}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 15:42
 * Description: Модели восприятия состояния пермишшенов.
 * @see [[https://developers.facebook.com/docs/facebook-login/permissions/v2.3]]
 */

object FbPermissions extends StringEnum[FbPermission] with MaybeFromJsonT {

  /** Разрешение на публикацию в профиле юзера и от имени юзера. */
  case object PublishActions extends FbPermission("publish_actions")

  /** Разрешение на публикацию постов на страницах. */
  case object PublishPages extends FbPermission("publish_pages")

  /** Разрешение на получение access_token'ов к страницам для создания публикаций от имени этих страниц
    * и других действий со страницами. */
  case object ManagePages extends FbPermission("manage_pages")

  /** Профиль текущего юзера. Автоматический пермишен. */
  case object PublicProfile extends FbPermission("public_profile")

  /** Доступ к фоткам и альбомам юзера. */
  case object UserPhotos extends FbPermission("user_photos")


  override def values = findValues


  override type T = FbPermission
  /** Десериализовать один пермишшен из json. Антипод для [[FbPermission]].toJson(). */
  override def maybeFromJson(raw: Any): Option[FbPermission] = {
    withValueOpt(raw.toString)
  }

  /** Десериализация списка прав из json. */
  def permsFromJson(raw: Any): Seq[FbPermission] = {
    raw.asInstanceOf[Array[Any]]
      .iterator
      .flatMap(maybeFromJson)
      .toSeq
  }


  def wantPublishPerms: List[FbPermission] =
    PublishActions :: PublishPages :: ManagePages :: Nil

}


sealed abstract class FbPermission(override val value: String) extends StringEnumEntry with IToJson {
  /** Идентификатор разрешения по мнению фейсбука. */
  @inline final def fbName: String = value
  override def toString = fbName
  override def toJson: Any = fbName
}

object FbPermission {

  /** Facebook принимает списки пермишшенов строкой через запятую.
    * Этот метод компилит список пермишшенов в строку. */
  def permsToString(perms: TraversableOnce[FbPermission]): String = {
    perms.toIterator
      .map(_.fbName)
      .mkString(",")
  }

  /** Сериализация списка пермишшенов в JSON. */
  def permsToJson(perms: TraversableOnce[FbPermission]): Array[Any] = {
    perms.toIterator
      .map { _.toJson }
      .toJSArray
  }

  implicit def fbPermissionFormat: Format[FbPermission] =
    EnumeratumUtil.valueEnumEntryFormat( FbPermissions )

  @inline implicit def univEq: UnivEq[FbPermission] = UnivEq.derive

}
