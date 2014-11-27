package models.blk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.14 11:48
 * Description: Реинкарнация модели, жившей внутри val BlocksUtil.FONT_SIZES_DFLT.
 */
object FontSizes extends Enumeration {

  sealed protected case class Val(size: Int, lineHeight: Int) extends super.Val(size)

  type FontSize = Val

  val F10: FontSize = Val(10, 8)
  val F12: FontSize = Val(12, 10)
  val F14: FontSize = Val(14, 12)
  val F16: FontSize = Val(16, 14)
  val F18: FontSize = Val(18, 16)
  val F22: FontSize = Val(22, 20)
  val F26: FontSize = Val(26, 24)
  val F30: FontSize = Val(30, 28)
  val F34: FontSize = Val(34, 30)
  val F38: FontSize = Val(38, 34)
  val F42: FontSize = Val(42, 38)
  val F46: FontSize = Val(46, 42)
  val F50: FontSize = Val(50, 46)
  val F54: FontSize = Val(54, 50)
  val F58: FontSize = Val(58, 54)
  val F62: FontSize = Val(62, 58)
  val F66: FontSize = Val(66, 62)
  val F70: FontSize = Val(70, 66)
  val F74: FontSize = Val(74, 70)
  val F80: FontSize = Val(80, 76)
  val F84: FontSize = Val(84, 80)

  implicit def value2val(x: Value): FontSize = x.asInstanceOf[FontSize]

  /** Сортированный список значений. */
  val valuesSorted: List[FontSize] = {
    values
      .iterator
      .map(value2val)
      .toList
      .sortBy(_.size)
  }

}
