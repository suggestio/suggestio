package models.blk

import io.suggest.model.EnumMaybeWithId
import util.FormUtil.IdEnumFormMappings

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 16:47
 * Description: Допустимые высоты блоков.
 */
object BlockHeights extends Enumeration with EnumMaybeWithId with IdEnumFormMappings {

  /**
   * Экземпляры модели.
   * @param heightPx Значение высоты в пикселях.
   * @param relSz Относительный (нормированный) размер по высоте.
   */
  protected case class Val(heightPx: Int, relSz: Int) extends super.Val(heightPx) with RelSz with IntParam {
    override def intValue = heightPx
  }

  override type T = Val

  val H140: T = Val(140, relSz = 1)
  val H300: T = Val(300, relSz = 2)
  val H460: T = Val(460, relSz = 3)
  val H620: T = Val(620, relSz = 4)

  def default = H300
  def max = H620
  def min = H140

  /**
   * Все высоты по возрастанию.
   * @return Последовательность высот, отсортированная по возрастанию.
   */
  val allSorted: Seq[T] = {
    values
      .toSeq
      .map(value2val)
      .sortBy(_.heightPx)
  }


  def maybeWithHeight(height: Int): Option[T] = {
    maybeWithId(height)
  }
  def withHeight(height: Int): T = {
    maybeWithHeight(height).get
  }

}
