package models.blk

import io.suggest.common.menum.{EnumMaybeWithId, EnumValue2Val}
import io.suggest.sc.tile.TileConstants
import util.FormUtil.IdEnumFormMappings

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 16:45
 * Description: Модель допустимых ширин блоков.
 */
object BlockWidths extends Enumeration with EnumValue2Val with EnumMaybeWithId with IdEnumFormMappings {

  /**
   * Экземпляр этой модели.
   * @param widthPx Ширина в пикселях.
   */
  sealed protected[this] abstract class Val(val widthPx: Int)
    extends super.Val(widthPx)
    with IntParam
    with RelSz
  {
    /** Нормированный размер в единицах размера. Для ширины по сути - 1 или 2. */
    def relSz: Int
    /** Узкий размер? */
    def isNarrow: Boolean
    override def intValue = widthPx
    override def toString(): String = s"[$widthPx]"
  }

  override type T = Val

  /** Самый узкий блок. */
  val NARROW: T = new Val(TileConstants.CELL_WIDTH_140_CSSPX) {
    override def relSz = 1
    override def isNarrow = true
  }

  /** Обычная ширина блока. */
  val NORMAL: T = new Val(TileConstants.CELL_WIDTH_300_CSSPX) {
    override def relSz = 2
    override def isNarrow = false
  }


  def default = NORMAL
  def max = NORMAL
  def min = NARROW

  def maybeWithWidth(width: Int): Option[T] = {
    maybeWithId(width)
  }
  def withWidth(width: Int): BlockWidth = {
    maybeWithWidth(width).get
  }

  /** Все допустимые ширИны по возрастанию. */
  val allSorted: Seq[T] = {
    values
      .toSeq
      .map(value2val)
      .sortBy(_.widthPx)
  }

}
