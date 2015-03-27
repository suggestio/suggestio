package io.suggest.xadv.ext.js.fb.m

import io.suggest.model.LightEnumeration

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 13:56
 * Description: Модель допустимых статусов залогиненности юзера в рамках facebook.
 */
object FbLoginStatuses extends LightEnumeration {

  /** Интерфейс экземпляров модели. */
  protected trait ValT extends super.ValT {
    val fbStatus: String
    override def toString = fbStatus

    /** Залогинен ли юзер в фейсбуке? */
    def isFbLoggedIn: Boolean

    /** Заапрувил ли текущее приложение по отношению к своему аккаунту? (без учета спец.прав) */
    def isAppConnected: Boolean
  }

  /** Экземпляры модели. */
  protected abstract sealed class Val(val fbStatus: String) extends ValT

  override type T = Val


  /** Юзер залогинен в фейсбуке и подключил приложение к своему аккаунту (без учета прав). */
  val Connected: T = new Val("connected") {
    override def isFbLoggedIn = true
    override def isAppConnected = true
  }

  /** Юзер отклонил приложение, но он залогинен в фейсбуке. */
  val NotAuthorized: T = new Val("not_authorized") {
    override def isFbLoggedIn = true
    override def isAppConnected = false
  }

  /** Юзер не залогинен в фейсбуке. */
  val Unknown: T = new Val("unknown") {
    override def isFbLoggedIn = false
    override def isAppConnected = false
  }


  override def maybeWithName(n: String): Option[T] = {
    n match {
      case Connected.fbStatus       => Some(Connected)
      case NotAuthorized.fbStatus   => Some(NotAuthorized)
      case Unknown.fbStatus         => Some(Unknown)
      case _                        => None
    }
  }

}
