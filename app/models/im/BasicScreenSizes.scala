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

  protected case class Val(width: Int, height: Int)
    extends super.Val(s"${width}x$height")
    with MImgSizeT
    with ImgOrientationT

  type BasicScreenSize = Val


  // 5:3 (~1.6)
  val FWQVGA_V: BasicScreenSize   = Val(width = 240, height = 400)
  val FWQVGA_H: BasicScreenSize   = Val(width = 400, height = 240)

  // 16:9 (~1.7)
  val NHD_V: BasicScreenSize      = Val(width = 360, height = 640)
  val NHD_H: BasicScreenSize      = Val(width = 640, height = 360)

  // 16:9
  val WVGA_V: BasicScreenSize     = Val(width = 480, height = 800)
  val WVGA_H: BasicScreenSize     = Val(width = 800, height = 480)

  // 16:10 (1.6), близкое к WSVGA, но безымянное разрешение.
  val WSVGA_V: BasicScreenSize    = Val(width = 640,  height = 1024)
  val WSVGA_H: BasicScreenSize    = Val(width = 1024, height = 640)

  // 16:10
  val WXGA_V: BasicScreenSize     = Val(width = 800,  height = 1280)
  val WXGA_H: BasicScreenSize     = Val(width = 1280, height = 800)

  // 16:10
  val WXGAPLUS_V: BasicScreenSize = Val(width = 900,  height = 1440)
  val WXGAPLUS_H: BasicScreenSize = Val(width = 1440, height = 900)

  // 16:9
  val FHD_V: BasicScreenSize      = Val(width = 1080, height = 1920)
  val FHD_H: BasicScreenSize      = Val(width = 1920, height = 1080)

  // 16:9
  val QWXGA_V: BasicScreenSize    = Val(width = 1152, height = 2048)
  val QWXGA_H: BasicScreenSize    = Val(width = 2048, height = 1152)

  // 16:10
  val WUXGA_V: BasicScreenSize    = Val(width = 1200, height = 1920)
  val WUXGA_H: BasicScreenSize    = Val(width = 1920, height = 1200)

  // 16:9
  val QHD_V: BasicScreenSize      = Val(width = 1440, height = 2560)
  val QHD_H: BasicScreenSize      = Val(width = 2569, height = 1440)

  // 16:10
  val WQXGA_V: BasicScreenSize    = Val(width = 1600, height = 2560)
  val WQXGA_H: BasicScreenSize    = Val(width = 2560, height = 1600)


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
      .getOrElse { if (sz.isHorizontal) FHD_H else FHD_V }
  }

}
