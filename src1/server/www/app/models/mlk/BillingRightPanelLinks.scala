package models.mlk

import enumeratum.{Enum, EnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.06.19 12:23
  * Description: Enum для задания параметра подсветки текущей ссылки на правой панели в разделе биллинга узла.
  */

object BillingRightPanelLinks extends Enum[BillingRightPanelLink] {
  case object RPL_BILLING extends BillingRightPanelLink
  case object RPL_CART extends BillingRightPanelLink
  case object RPL_ORDERS extends BillingRightPanelLink
  override val values = findValues
}


sealed abstract class BillingRightPanelLink extends EnumEntry

object BillingRightPanelLink {
  @inline implicit def univEq: UnivEq[BillingRightPanelLink] = UnivEq.derive
}
