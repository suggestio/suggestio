package models.im

import java.awt.Color
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.12.14 15:55
 * Description: Модели представления графических примитивов: точек, цветов и т.д.
 */


/** Интерфейс цветовой точки в абстрактном 3-мерном пространстве цветов. */
trait ColorPoint3D {
  def x: Int
  def y: Int
  def z: Int
}


/** Цвет-точка в 3-мерном пространстве цветов RGB. */
case class RGB(red: Int, green: Int, blue: Int) extends ColorPoint3D {
  override def x = red
  override def y = green
  override def z = blue
}

object RGB {
  /**
   * Парсер из hex в [[RGB]].
   * @param colorStr hex-строка вида "FFAA33" или "#FFAA33".
   * @return Инстанс RGB.
   *         Exception, если не удалось строку осилить.
   */
  def hex2rgb(colorStr: String): RGB = {
    val cs1 = if (colorStr startsWith "#")
      colorStr
    else
      "#" + colorStr
    val c = Color.decode(cs1)
    RGB(c.getRed, c.getGreen, c.getBlue)
  }

  def apply(hex: String) = hex2rgb(hex)
}


/**
 * Распарсенный ряд гистограммы. Включает в себя абсолютную частоту и код цвета.
 * @param frequency Кол-во пикселей с указанным цветом.
 * @param colorHex HEX-код цвета в виде строки: "FFFFFF".
 */
case class HistogramEntry(frequency: Long, colorHex: String, rgb: RGB)

/**
 * Обертка для коллекции с данными гистограммы. Полезна при передаче гистограммы
 * с помощью акторов.
 * @param sorted Отсортированная гистограмма.
 */
case class Histogram(sorted: List[HistogramEntry])
