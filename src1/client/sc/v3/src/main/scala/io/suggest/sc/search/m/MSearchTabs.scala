package io.suggest.sc.search.m

import enumeratum._
import io.suggest.i18n.MsgCodes

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 13:56
  * Description: Статическая enum-модель тегов.
  */
sealed abstract class MSearchTab extends EnumEntry {

  /** Самая первая (левая) вкладка. */
  def isFirst : Boolean

  /** Самая последняя (правая) вкладка. */
  def isLast  : Boolean

  /** i18n-код названия вкладки. */
  def name: String

  override final def toString = name

}


/** Модель табов панели поиска. */
object MSearchTabs extends Enum[MSearchTab] {

  /** Вкладка с географической картой. */
  case object GeoMap extends MSearchTab {
    override def isFirst = true
    override def isLast  = false
    override def name    = MsgCodes.`Map`
  }

  /** Вкладка с тегами. */
  case object Tags extends MSearchTab {
    override def isFirst = false
    override def isLast  = true
    override def name    = MsgCodes.`Tags`
  }


  /** Все значения модели. */
  override val values = findValues

  def default: MSearchTab = GeoMap

}
