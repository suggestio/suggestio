package io.suggest.id.login.m

import enumeratum.values.{IntEnum, IntEnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 17:34
  * Description: Табы на странице логина.
  */
object MLoginTabs extends IntEnum[MLoginTab] {

  /** Таб со страницей ввода имени-пароля. */
  case object Epw extends MLoginTab(1)


  // TODO Госуслуги!
  def default: MLoginTab = Epw

  override def values = findValues

}


sealed abstract class MLoginTab(override val value: Int) extends IntEnumEntry

object MLoginTab {

  @inline implicit def univEq: UnivEq[MLoginTab] = UnivEq.derive

}
