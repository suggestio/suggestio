package io.suggest.ad.blk

import enumeratum.values.IntEnum
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 16:45
 * Description: Модель допустимых ширин блоков.
 */


/** Класс одного элемента модели. */
sealed abstract class BlockWidth(override val value: Int) extends IBlockSize {

  /** Узкий размер? */
  def isNarrow: Boolean

  override final def toString = value.toString

}


/** Модель допустимых ширин блока. */
case object BlockWidths extends IntEnum[BlockWidth] with IBlockSizes[BlockWidth] {

  // TODO Взять значения из IBlockSize.S*. Сейчас оно не компилится даже с final.

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

  def default          : BlockWidth = NORMAL
  override def max     : BlockWidth = NORMAL
  override def min     : BlockWidth = NARROW

}


object BlockWidth {

  /** Поддержка play-json. */
  implicit val BLOCK_WIDTH_FORMAT: Format[BlockWidth] = {
    EnumeratumUtil.valueEnumEntryFormat( BlockWidths )
  }

  implicit def univEq: UnivEq[BlockWidth] = UnivEq.derive

}

