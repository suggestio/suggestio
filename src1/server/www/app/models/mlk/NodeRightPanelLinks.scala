package models.mlk

import enumeratum.{Enum, EnumEntry}
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.06.19 11:59
  * Description: Модель кнопок правой панели личного кабинета.
  */

object NodeRightPanelLinks extends Enum[NodeRightPanelLink] {
  case object RPL_NODE extends NodeRightPanelLink
  case object RPL_NODE_EDIT extends NodeRightPanelLink
  case object RPL_USER_EDIT extends NodeRightPanelLink
  case object RPL_ADN_MAP extends NodeRightPanelLink
  case object RPL_NODES extends NodeRightPanelLink

  override val values = findValues
}


/** Enum для задания параметра подсветки текущей ссылки на правой панели личного кабинета узла. */
sealed abstract class NodeRightPanelLink extends EnumEntry

object NodeRightPanelLink {
  implicit def univEq: UnivEq[NodeRightPanelLink] = UnivEq.derive
}
