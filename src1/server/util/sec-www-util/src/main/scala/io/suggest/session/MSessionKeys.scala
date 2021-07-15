package io.suggest.session

import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.15 14:21
 * Description: Модель для хранения ключей доступа к сессии.
 */

object MSessionKeys extends StringEnum[MSessionKey] {

  /** Session-поле timestamp'а для контроля истечения сессии. */
  case object Timestamp extends MSessionKey("t")

  /** Session-поле для хранения текущего person_id. */
  case object PersonId extends MSessionKey("p")

  /** Флаг для долгого хранения залогиненности.*/
  case object RememberMe extends MSessionKey("r")

  /** Изначально было ключом к secure-social, который сохраняет ссылку для возврата юзера через session. */
  case object ExtLoginData extends MSessionKey("x")

  /** Ignore and suppress super-user capabilities. Super-user becomes normal user. */
  case object NoSu extends MSessionKey("u")

  override val values = findValues

  def onlyLoginIter: Iterator[MSessionKey] =
    values.iterator.filter(_.isLogin)

  def removeOnLogout: List[MSessionKey] =
    PersonId :: Timestamp :: NoSu :: ExtLoginData :: Nil

}


sealed abstract class MSessionKey(override val value: String) extends StringEnumEntry {
  override final def toString = value
}

object MSessionKey {

  @inline implicit def univEq: UnivEq[MSessionKey] = UnivEq.derive

  implicit class MSessionKeyOpsExt( val msk: MSessionKey ) extends AnyVal {

    /** Относится ли сессионный ключ к Login-набору? */
    def isLogin: Boolean = {
      msk match {
        // remember-me кэшируется между логинами.
        case MSessionKeys.ExtLoginData | MSessionKeys.RememberMe =>
          false
        case _ => true
      }
    }

  }

}

