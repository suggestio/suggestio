package io.suggest.xadv.ext.js.fb.m

import io.suggest.model.LightEnumeration
import io.suggest.xadv.ext.js.runner.m.{MaybeFromJsonT, IToJson}

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.{Array, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 15:42
 * Description: Модели восприятия состояния пермишшенов.
 * @see [[https://developers.facebook.com/docs/facebook-login/permissions/v2.3]]
 */

object FbPermissions extends LightEnumeration with MaybeFromJsonT {

  protected trait ValT extends super.ValT with IToJson {
    /** Идентификатор разрешения по мнению фейсбука. */
    val fbName: String
    override def toString = fbName
    override def toJson: Any = fbName
  }

  /** Экземпляр модели. */
  protected sealed class Val(val fbName: String) extends ValT {

    override def equals(obj: scala.Any): Boolean = {
      super.equals(obj) || {
        obj match {
          case v: Val   => v.fbName == fbName
          case _        => false
        }
      }
    }

  }

  override type T = Val


  /** Разрешение на публикацию в профиле юзера и от имени юзера. */
  val PublishActions: T         = new Val("publish_actions")

  /** Разрешение на публикацию постов на страницах. */
  val PublishPages: T           = new Val("publish_pages")

  /** Разрешение на получение access_token'ов к страницам для создания публикаций от имени этих страниц
    * и других действий со страницами. */
  val ManagePages: T            = new Val("manage_pages")

  /** Профиль текущего юзера. Автоматический пермишен. */
  lazy val PublicProfile: T     = new Val("public_profile")

  /** Доступ к фоткам и альбомам юзера. */
  lazy val UserPhotos: T        = new Val("user_photos")


  override def maybeWithName(n: String): Option[T] = {
    n match {
      case PublishActions.fbName => Some(PublishActions)
      case PublishPages.fbName   => Some(PublishPages)
      case ManagePages.fbName    => Some(ManagePages)
      case PublicProfile.fbName  => Some(PublicProfile)
      case UserPhotos.fbName     => Some(UserPhotos)
      case _                     => None    // TODO Логгировать неизвесные пермишены. Желательно, прямо на сервере.
    }
  }

  /** Десериализовать один пермишшен из json. Антипод для [[ValT]].toJson(). */
  override def maybeFromJson(raw: Any): Option[T] = {
    maybeWithName(raw.toString)
  }

  /** Facebook принимает списки пермишшенов строкой через запятую.
    * Этот метод компилит список пермишшенов в строку. */
  def permsToString(perms: TraversableOnce[T]): String = {
    perms.toIterator
      .map(_.fbName)
      .mkString(",")
  }

  /** Сериализация списка пермишшенов в JSON. */
  def permsToJson(perms: TraversableOnce[T]): Array[Any] = {
    perms.toIterator
      .map { _.toJson }
      .toJSArray
  }

  /** Десериализация списка прав из json. */
  def permsFromJson(raw: Any): Seq[T] = {
    raw.asInstanceOf[Array[Any]]
      .iterator
      .flatMap(maybeFromJson)
      .toSeq
  }


  def wantPublishPerms = Seq(PublishActions, PublishPages, ManagePages)

}
