package models.msession

import io.suggest.model.EnumMaybeWithName
import securesocial.core.SecureSocial

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.15 14:21
 * Description: Модель для хранения ключей доступа к сессии.
 */
object Keys extends Enumeration with EnumMaybeWithName {

  protected sealed class Val(val name: String, val isLogin: Boolean) extends super.Val(name)

  override type T = Val

  /** Session-поле timestamp'а для контроля истечения сессии. */
  val Timestamp     : T = new Val("t", true)

  /** Session-поле для хранения текущего person_id. */
  val PersonId      : T = new Val("p", true)

  /** Флаг для долгого хранения залогиненности.*/
  val RememberMe    : T = new Val("r", true)

  /** Костыль к secure-social сохраняет ссылку для возврата юзера через session.
    * Менять на что-то отличное от оригинала пока нельзя. */
  val OrigUrl       : T = new Val(SecureSocial.OriginalUrlKey, false)


  def onlyLoginIter = values.iterator.filter(_.isLogin)

}
