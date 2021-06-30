package io.suggest.id.login

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import io.suggest.i18n.MsgCodes
import io.suggest.url.bind._
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

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

  //case object PasswordChange extends MLoginTab(3) {
  //  override def msgCode = MsgCodes.`Password.change`
  //}

  def default: MLoginTab = EpwLogin

  override def values = findValues

}


sealed abstract class MLoginTab(override val value: Int) extends IntEnumEntry {
  def msgCode: String
}

object MLoginTab {

  @inline implicit def univEq: UnivEq[MLoginTab] = UnivEq.derive

  implicit def loginTabJson: Format[MLoginTab] =
    EnumeratumUtil.valueEnumEntryFormat( MLoginTabs )

  implicit def loginTabQsB(implicit intB: QsBindable[Int]): QsBindable[MLoginTab] =
    EnumeratumUtil.qsBindable( MLoginTabs )

}
