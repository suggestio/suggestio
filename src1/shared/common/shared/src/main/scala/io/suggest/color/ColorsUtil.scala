package io.suggest.color

import scala.annotation.tailrec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.06.2020 15:23
  * Description: Утиль для работы с цветами.
  * Начальный код эвакуирован со свалки TplDataFormatUtil в качестве ненужного трофея.
  */
object ColorsUtil {

  /** Сконвертить "ffffff" в List(255,255,255). */
  def colorHex2rgb(hex: String): List[Int] = {
    colorHex2rgb(hex, 0, Nil)
  }
  @tailrec def colorHex2rgb(hex: String, start: Int, acc: List[Int]): List[Int] = {
    if (hex startsWith "#") {
      colorHex2rgb(hex.tail)
    } else if (start > hex.length - 1) {
      acc.reverse
    } else {
      val untilPos = start + 2
      val subhex = hex.substring(start, untilPos)
      val xint = Integer.parseInt(subhex, 16)
      colorHex2rgb(hex, untilPos, xint :: acc)
    }
  }


  /** Отрендерить набор цветов в rgb(...) или rgba(...) цвет для css-значений. */
  def formatRgbHexColorCss(colorHex: String, withOpacity: Option[Float] = None): String = {
    formatRgbColorCss(colorHex2rgb(colorHex), withOpacity)
  }
  def formatRgbColorCss(rgb: Iterable[Int], withOpacity: Option[Float] = None): String = {
    val withOp = withOpacity.isDefined
    val l0 = if (withOp) 22 else 16
    val sb = new StringBuilder(l0, "rgb")
    if (withOp)
      sb.append('a')
    sb.append('(')
    for (c <- rgb) {
      sb.append(c)
        .append(',')
    }
    withOpacity.fold(
      sb.setLength( sb.length() - 1 )
    )( sb.append(_) )

    sb.append(')')
      .toString
  }


  def colorRgb2Hsl(rgb: List[Int]): List[Int] = {

    val r: Float = rgb(0).toFloat / 255
    val g: Float = rgb(1).toFloat / 255
    val b: Float = rgb(2).toFloat / 255

    val rgbSorted = List(r,g,b).sortWith(_ < _)

    val max = rgbSorted(2)
    val min = rgbSorted(0)

    if (max == min) {
      List( 0, 0, ((max + min)/2*100).toInt)
    } else {

      val l = ( max + min ) / 2

      val s = if( l < 0.5 ){
        (max - min) / (max + min)
      } else {
        (max - min) / (2.0 - max - min)
      }

      val h = if( r == max ){
        (g - b) / (max - min)
      } else if( g == max ){
        2.0 + (b - r) / ( max - min )
      } else {
        4.0 + (r - g) / ( max - min )
      }

      val h1 = if( h < 0 ) h*60 + 360 else h*60

      List( h1.toInt, (s*100).toInt, (l*100).toInt )

    }

  }

}
