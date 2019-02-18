package models.adv

import enumeratum.{Enum, EnumEntry}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.11.15 15:24
 * Description: Модель ссылок правой панели в ЛК при размещении карточки.
 */
object LkAdvRightLinks extends Enum[LkAdvRightLink] {

  case object GEO extends LkAdvRightLink
  case object EXT extends LkAdvRightLink
  case object AD_NODES extends LkAdvRightLink

  override val values = findValues

}

sealed abstract class LkAdvRightLink extends EnumEntry
