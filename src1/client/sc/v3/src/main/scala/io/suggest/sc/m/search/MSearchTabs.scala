package io.suggest.sc.m.search

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.i18n.MsgCodes
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 13:56
  * Description: Статическая enum-модель тегов.
  */

/** Модель табов панели поиска. */
object MSearchTabs extends StringEnum[MSearchTab] {

  /** Вкладка с географической картой. */
  case object GeoMap extends MSearchTab("g") {
    override def isFirst = true
    override def isLast  = false
    override def name    = MsgCodes.`Map`
  }

  /** Вкладка с тегами. */
  case object Tags extends MSearchTab("h") {
    override def isFirst = false
    override def isLast  = true
    override def name    = MsgCodes.`Tags`
  }


  /** Все значения модели. */
  override val values = findValues

  def default: MSearchTab = GeoMap

}


/** Класс одного элемента модели [[MSearchTabs]].
  * Являет собой одну вкладку search-панели. */
sealed abstract class MSearchTab(override val value: String) extends StringEnumEntry {

  /** Самая первая (левая) вкладка. */
  def isFirst : Boolean

  /** Самая последняя (правая) вкладка. */
  def isLast  : Boolean

  /** i18n-код названия вкладки. */
  def name: String

  override final def toString = name

}

object MSearchTab {
  implicit def univEq: UnivEq[MSearchTab] = UnivEq.derive
}

