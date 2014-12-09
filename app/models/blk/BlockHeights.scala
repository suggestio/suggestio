package models.blk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 16:47
 * Description: Допустимые высоты блоков.
 */
object BlockHeights extends Enumeration {

  /**
   * Экземпляры модели.
   * @param heightPx Значение высоты в пикселях.
   * @param relSz Относительный (нормированный) размер по высоте.
   */
  protected case class Val(heightPx: Int, relSz: Int) extends super.Val(heightPx) with RelSz with IntParam {
    override def intValue = heightPx
  }

  type BlockHeight = Val

  val H140: BlockHeight = Val(140, relSz = 1)
  val H300: BlockHeight = Val(300, relSz = 2)
  val H460: BlockHeight = Val(460, relSz = 3)
  val H620: BlockHeight = Val(620, relSz = 4)

  implicit def value2val(x: Value): BlockHeight = x.asInstanceOf[BlockHeight]

  def default = H300
  def max = H620
  def min = H140

  /**
   * Все высоты по возрастанию.
   * @return Последовательность высот, отсортированная по возрастанию.
   */
  val allSorted: Seq[BlockHeight] = {
    values
      .toSeq
      .map(value2val)
      .sortBy(_.heightPx)
  }


  def maybeWithHeight(height: Int): Option[BlockHeight] = {
    values
      .iterator
      .map(value2val)
      .find(_.intValue == height)
  }
  def withHeight(width: Int): BlockHeight = {
    maybeWithHeight(width).get
  }

}
