package models.im

import io.suggest.ym.model.common.MImgSizeT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.14 12:27
 * Description: Базовые размеры экранов.
 * @see [[http://en.wikipedia.org/wiki/Comparison_of_Android_devices]].
 */
object BasicScreenSizes extends Enumeration {

  protected case class Val(width: Int, height: Int) extends super.Val with MImgSizeT

  type BasicScreenSize = Val

  val QVGA_V: BasicScreenSize     = Val(width = 240, height = 320)
  val QVGA_H: BasicScreenSize     = Val(width = 320, height = 240)

  val HVGA_V: BasicScreenSize     = Val(width = 320, height = 480)
  val HVGA_H: BasicScreenSize     = Val(width = 480, height = 320)

  val WVGA_V: BasicScreenSize     = Val(width = 480, height = 800)
  val WVGA_H: BasicScreenSize     = Val(width = 800, height = 480)

  val XGA_H: BasicScreenSize      = Val(width = 1024, height = 768)
  val XGA_V: BasicScreenSize      = Val(width = 768, height = 1024)

  val HD720_H: BasicScreenSize    = Val(width = 1280, height = 720)
  val HD720_V: BasicScreenSize    = Val(width = 720, height = 1280)

  val HD1080_H: BasicScreenSize   = Val(width = 1920, height = 1080)
  val HD1080_V: BasicScreenSize   = Val(width = 1080, height = 1920)


  implicit def value2val(x: Value): BasicScreenSize = x.asInstanceOf[BasicScreenSize]

  /** Найти подходящее разрешение, если есть. */
  def includesSize(sz: MImgSizeT): Option[BasicScreenSize] = {
    values
      .find { _ isIncudesSz sz }
      .map { value2val }
  }

  /** Найти подходящее разрешение или выбрать максимальное. */
  def includesSizeOrMax(sz: MImgSizeT): BasicScreenSize = {
    includesSize(sz)
      .getOrElse { if (sz.isHorizontal) HD1080_H else HD1080_V }
  }

}
