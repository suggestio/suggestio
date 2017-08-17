package io.suggest.ad.blk

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import play.api.libs.json.Format

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 16:45
 * Description: Модель допустимых ширин блоков.
 */
object BlockWidth {

  /** Поддержка play-json. */
  implicit val BLOCK_WIDTH_FORMAT: Format[BlockWidth] = {
    EnumeratumUtil.valueEnumEntryFormat( BlockWidths )
  }

}


/** Класс одного элемента модели. */
sealed abstract class BlockWidth(override val value: Int) extends IntEnumEntry {

  /** Нормированный размер в единицах размера. Для ширины по сути - 1 или 2. */
  def relSz: Int

  /** Узкий размер? */
  def isNarrow: Boolean

  override final def toString = {
    s"[$value]"
  }

}


/** Модель допустимых ширин блока. */
object BlockWidths extends IntEnum[BlockWidth] {

  /** Самый узкий блок. */
  case object NARROW extends BlockWidth( 140 ) {
    override def relSz = 1
    override def isNarrow = true
  }

  /** Обычная ширина блока. */
  case object NORMAL extends BlockWidth( 300 ) {
    override def relSz = 2
    override def isNarrow = false
  }

  override val values = findValues

  def default : BlockWidth = NORMAL
  def max     : BlockWidth = NORMAL
  def min     : BlockWidth = NARROW

}
