package models.blk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.14 11:48
 * Description: Реинкарнация модели, жившей внутри val BlocksUtil.FONT_SIZES_DFLT.
 */
object FontSizes extends Enumeration {

  /**
   * Экземпляр модели.
   * @param size css font-size.
   * @param lineHeight css line-height.
   */
  sealed protected class Val(val size: Int, val lineHeight: Int) extends super.Val(size) {
    def isLast: Boolean = false
  }

  /** Тип экземпляра модели. */
  type FontSize = Val

  // Значения модели.
  val F10: FontSize = new Val(10, 8)
  val F12: FontSize = new Val(12, 10)
  val F14: FontSize = new Val(14, 12)
  val F16: FontSize = new Val(16, 14)
  val F18: FontSize = new Val(18, 16)
  val F22: FontSize = new Val(22, 20)
  val F26: FontSize = new Val(26, 24)
  val F30: FontSize = new Val(30, 28)
  val F34: FontSize = new Val(34, 30)
  val F38: FontSize = new Val(38, 34)
  val F42: FontSize = new Val(42, 38)
  val F46: FontSize = new Val(46, 42)
  val F50: FontSize = new Val(50, 46)
  val F54: FontSize = new Val(54, 50)
  val F58: FontSize = new Val(58, 54)
  val F62: FontSize = new Val(62, 58)
  val F66: FontSize = new Val(66, 62)
  val F70: FontSize = new Val(70, 66)
  val F74: FontSize = new Val(74, 70)
  val F80: FontSize = new Val(80, 76)
  val F84: FontSize = new Val(84, 80) {
    override def isLast: Boolean = true
  }

  /** Приведение Enumeration.Value к экземпляру модели. */
  implicit def value2val(x: Value): FontSize = x.asInstanceOf[FontSize]

  /** Сортированный список значений FontSize.
    * values() тоже вроде бы сортирован, но по строковому имени, и является Set[Value]. А нам надо по id сортировку. */
  val valuesSorted: List[FontSize] = {
    values
      .iterator
      .map(value2val)
      .toList
      .sortBy(_.size)
  }

}
