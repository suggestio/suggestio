package io.suggest.id.login

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.i18n.MsgCodes
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 17:34
  * Description: Табы на странице логина.
  */
object MLoginTabs extends IntEnum[MLoginTab] {

  /** Таб с логином через внешние сервисы. */
  case object Ext extends MLoginTab( 0 ) {
    override def msgCode = MsgCodes.`Login`
  }

  /** Таб со страницей ввода имени-пароля. */
  case object EpwLogin extends MLoginTab(1) {
    override def msgCode = MsgCodes.`Login.page.title`
  }

  /** Таб с регистрацией по email-паролю. */
  case object Reg extends MLoginTab(2) {
    override def msgCode = MsgCodes.`Sign.up`
  }


  def default: MLoginTab = EpwLogin

  override def values = findValues

}


sealed abstract class MLoginTab(override val value: Int) extends IntEnumEntry {
  def msgCode: String
}

object MLoginTab {

  @inline implicit def univEq: UnivEq[MLoginTab] = UnivEq.derive

}
