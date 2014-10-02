package models.im

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.14 12:27
 * Description: Базовые размеры экранов.
 * @see [[http://en.wikipedia.org/wiki/Comparison_of_Android_devices]]
 */
object BasicScreenSizes extends Enumeration {

  protected case class Val(width: Int, height: Int) extends super.Val {
    def isHorizontal = width > height
    def isVertical = height > width
  }

  val QVGA_V = Val(width = 240, height = 320)
  val QVGA_H = Val(width = 320, height = 240)

  val HVGA_V = Val(width = 320, height = 480)
  val HVGA_H = Val(width = 480, height = 320)

  val WVGA_V = Val(width = 480, height = 800)
  val WVGA_H = Val(width = 800, height = 480)

  val XGA_H  = Val(width = 1024, height = 768)
  val XGA_V  = Val(width = 768, height = 1024)

  val HD720_H = Val(width = 1280, height = 720)
  val HD720_V = Val(width = 720, height = 1280)

}
