package models.im

import java.awt.Color

import io.suggest.common.geom.coord.MCoords3d

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 16:51
  * Description: Модель RGB-цвета в виде int-триплета.
  */

/** Цвет-точка в 3-мерном пространстве цветов RGB. */
case class MRgb(red: Int, green: Int, blue: Int) {
  def toCoord3d = MCoords3d(x = red, y = green, z = blue)
}


object MRgb {

  /**
   * Парсер из hex в [[MRgb]].
   *
   * @param colorStr hex-строка вида "FFAA33" или "#FFAA33".
   * @return Инстанс RGB.
   *         Exception, если не удалось строку осилить.
   */
  def hex2rgb(colorStr: String): MRgb = {
    // TODO Задейстсовать MColorData().hexCode?
    val cs1 = if (colorStr startsWith "#")
      colorStr
    else
      "#" + colorStr
    // TODO Использовать Integer.parseInt("4F", 16)
    val c = Color.decode(cs1)
    MRgb(c.getRed, c.getGreen, c.getBlue)
  }

  def apply(hex: String) = hex2rgb(hex)

}

