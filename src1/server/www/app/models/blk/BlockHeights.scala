package models.blk

import enumeratum.values.{IntEnum, IntEnumEntry}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 16:47
 * Description: Допустимые высоты блоков.
 */
sealed abstract class BlockHeight(override val value: Int) extends IntEnumEntry {

  /** Относительный размер в условных "шагах". */
  def relSz: Int

}


/** Модель высот блока. */
object BlockHeights extends IntEnum[BlockHeight] {

  case object H140 extends BlockHeight(140) {
    override def relSz    = 1
  }

  case object H300 extends BlockHeight(300) {
    override def relSz    = 2
  }

  case object H460 extends BlockHeight(460) {
    override def relSz    = 3
  }

  case object H620 extends BlockHeight(620) {
    override def relSz    = 4
  }

  override val values = findValues

  def default : BlockHeight = H300
  def max     : BlockHeight = values.last
  def min     : BlockHeight = values.head

  /** Высота, после которой в adsListTpl применяется zigZag mask, для сокрытия всего, что не влезает. */
  def adsListMinZzHeight: BlockHeight = H300

}
