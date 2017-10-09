package models.im

import io.suggest.color.MRgb

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.14 15:55
  * Description: Обертка для коллекции с данными гистограммы.
  * Полезна при передаче гистограммы между акторами, у которых динамическая типизация в receive().
  *
  * @param sorted Отсортированная гистограмма.
  */

case class MHistogram(
                       sorted: List[MHistogramEntry]
                     )


/**
 * Распарсенный ряд гистограммы. Включает в себя абсолютную частоту и код цвета.
 * @param frequency Кол-во пикселей с указанным цветом.
 * @param colorHex HEX-код цвета в виде строки: "FFFFFF".
 */
case class MHistogramEntry(
                            frequency   : Long,
                            colorHex    : String,
                            rgb         : MRgb
                          ) {

  /** Теоретически возможно целочисленное переполнение в ImageMagic, которое выльется
    * в отрицательную частоту цвета. Защищаемся от этого с помощью нормирования частоты снизу. */
  def frequencyVerySafe = Math.max(0L, frequency)

}
