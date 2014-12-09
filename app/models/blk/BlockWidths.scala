package models.blk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 16:45
 * Description: Модель допустимых ширин блоков.
 */

/** Допустимые значения ширин блоков. */
object BlockWidths extends Enumeration {

  /**
   * Экземпляр этой модели.
   * @param widthPx Ширина в пикселях.
   * @param relSz Нормированный размер в единицах размера. Для ширины по сути - 1 или 2.
   */
  protected case class Val(widthPx: Int, relSz: Int, isNarrow: Boolean) extends super.Val(widthPx) with IntParam with RelSz {
    override def intValue = widthPx
  }

  type BlockWidth = Val

  val NARROW: BlockWidth = Val(140, relSz = 1, isNarrow = true)
  val NORMAL: BlockWidth = Val(300, relSz = 2, isNarrow = false)

  implicit def value2val(x: Value): BlockWidth = x.asInstanceOf[BlockWidth]

  def default = NORMAL
  def max = NORMAL
  def min = NARROW

  def maybeWithWidth(width: Int): Option[BlockWidth] = {
    values
      .iterator
      .map(value2val)
      .find(_.intValue == width)
  }
  def withWidth(width: Int): BlockWidth = {
    maybeWithWidth(width).get
  }

  /** Все допустимые ширИны по возрастанию. */
  val allSorted: Seq[BlockWidth] = {
    values
      .toSeq
      .map(value2val)
      .sortBy(_.widthPx)
  }

}
