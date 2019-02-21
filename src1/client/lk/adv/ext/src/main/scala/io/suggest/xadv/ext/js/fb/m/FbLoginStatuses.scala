package io.suggest.xadv.ext.js.fb.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.03.15 13:56
 * Description: Модель допустимых статусов залогиненности юзера в рамках facebook.
 */
object FbLoginStatuses extends StringEnum[FbLoginStatus] {

  /** Юзер залогинен в фейсбуке и подключил приложение к своему аккаунту (без учета прав). */
  case object Connected extends FbLoginStatus("connected") {
    override def isFbLoggedIn = true
    override def isAppConnected = true
  }

  /** Юзер отклонил приложение, но он залогинен в фейсбуке. */
  case object NotAuthorized extends FbLoginStatus("not_authorized") {
    override def isFbLoggedIn = true
    override def isAppConnected = false
  }

  /** Юзер не залогинен в фейсбуке. */
  case object Unknown extends FbLoginStatus("unknown") {
    override def isFbLoggedIn = false
    override def isAppConnected = false
  }


  override def values = findValues

}


sealed abstract class FbLoginStatus(override val value: String) extends StringEnumEntry {
  @inline final def fbStatus = value

  override final def toString = value

  /** Залогинен ли юзер в фейсбуке? */
  def isFbLoggedIn: Boolean

  /** Заапрувил ли текущее приложение по отношению к своему аккаунту? (без учета спец.прав) */
  def isAppConnected: Boolean
}


object FbLoginStatus {
  @inline implicit def univEq: UnivEq[FbLoginStatus] = UnivEq.derive
}
