package models.mlk

import enumeratum.{Enum, EnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.06.19 12:31
  * Description: Enum для задания параметра подсветки текущей ссылки на левой панели ЛК.
  */
object LkLeftPanelLinks extends Enum[LkLeftPanelLink] {
  case object LPL_NODE extends LkLeftPanelLink
  case object LPL_ADS extends LkLeftPanelLink
  case object LPL_MDR extends LkLeftPanelLink
  case object LPL_BILLING extends LkLeftPanelLink
  case object LPL_SUPPORT extends LkLeftPanelLink

  override val values = findValues
}


sealed abstract class LkLeftPanelLink extends EnumEntry

object LkLeftPanelLink {
  @inline implicit def univEq: UnivEq[LkLeftPanelLink] = UnivEq.derive
}
