package io.suggest.xadv.ext.js.fb.m

import java.net.URI

import io.suggest.model.LightEnumeration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.15 13:48
 * Description: Модель представления цели размещения на основе ссылки.
 */
object FbTarget {

  /**
   * Десериализовать из URL.
   * @param url Ссылка.
   * @return Экземпляр [[IFbTarget]].
   */
  def fromUrl(url: String): IFbTarget = {
    val path = new URI(url).getPath
    val groupRe = "/groups/([^/?&]+)".r
    path match {
      case groupRe(groupId) => FbTgGroupId(groupId)
      case _                => FbTgMe()
    }
  }

}


/** Интерфейс экземпляра модели. */
trait IFbTarget {
  /** Идентификатор объекта. */
  def id: String

  /** Тип объекта фейсбука. */
  def fbType: FbTgType
}

/** Группа фейсбука и её id. */
case class FbTgGroupId(id: String) extends IFbTarget {
  override def fbType = FbTgTypes.Group
}

/** Самому себе постим. */
case class FbTgMe() extends IFbTarget {
  override def id = "me"
  override def fbType = FbTgTypes.User
}


/** Типы объектов FB. */
object FbTgTypes extends LightEnumeration {
  protected sealed class Val(val strId: String) extends ValT

  override type T = Val

  object Group extends Val("g")
  object User  extends Val("u")

  // TODO Добавить типы page и event. + реализацию в парсинге.

  override def maybeWithName(n: String): Option[T] = {
    n match {
      case Group.strId => Some(Group)
      case User.strId  => Some(User)
      case _           => None
    }
  }
}
