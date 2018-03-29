package io.suggest.sec.m.msession

import enumeratum.values.{StringEnum, StringEnumEntry}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.15 14:21
 * Description: Модель для хранения ключей доступа к сессии.
 */

object Keys extends StringEnum[SessionKey] {

  /** Session-поле timestamp'а для контроля истечения сессии. */
  case object Timestamp extends SessionKey("t")

  /** Session-поле для хранения текущего person_id. */
  case object PersonId extends SessionKey("p")

  /** Флаг для долгого хранения залогиненности.*/
  case object RememberMe extends SessionKey("r")


  /** Костыль к secure-social сохраняет ссылку для возврата юзера через session.
    * Менять на что-то отличное от оригинала можно только после проверки controllers.ident.ExternalLogin.
    * на безопасность переименования. */

  case object OrigUrl extends SessionKey("original-url") {
    // TODO value = SecureSocial.OriginalUrlKey // Но Enumeratum пока не умеет final val.
    override def isLogin  = false
  }

  override val values = findValues

  def onlyLoginIter = values.iterator.filter(_.isLogin)

}


sealed abstract class SessionKey(override val value: String) extends StringEnumEntry {
  def isLogin: Boolean = true
  override final def toString = value
}

