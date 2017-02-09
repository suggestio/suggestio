package models.msession

import securesocial.core.SecureSocial
import enumeratum._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.15 14:21
 * Description: Модель для хранения ключей доступа к сессии.
 */
sealed abstract class SessionKey extends EnumEntry {
  def isLogin: Boolean = true
  def name = toString
}

object Keys extends Enum[SessionKey] {

  /** Session-поле timestamp'а для контроля истечения сессии. */
  case object Timestamp extends SessionKey {
    override def toString = "t"
  }

  /** Session-поле для хранения текущего person_id. */
  case object PersonId extends SessionKey {
    override def toString = "p"
  }

  /** Флаг для долгого хранения залогиненности.*/
  case object RememberMe extends SessionKey {
    override def toString = "r"
  }

  /** Костыль к secure-social сохраняет ссылку для возврата юзера через session.
    * Менять на что-то отличное от оригинала можно только после проверки [[controllers.ident.ExternalLogin]]
    * на безопасность переименования. */
  case object OrigUrl extends SessionKey {
    override def toString = SecureSocial.OriginalUrlKey
    override def isLogin = false
  }

  override val values = findValues

  def onlyLoginIter = values.iterator.filter(_.isLogin)

}
