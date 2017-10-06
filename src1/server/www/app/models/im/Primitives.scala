package models.im

import java.awt.Color

import io.suggest.common.geom.coord.MCoords3d
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.12.14 15:55
 * Description: Модели представления графических примитивов: точек, цветов и т.д.
 */



/**
 * Распарсенный ряд гистограммы. Включает в себя абсолютную частоту и код цвета.
 * @param frequency Кол-во пикселей с указанным цветом.
 * @param colorHex HEX-код цвета в виде строки: "FFFFFF".
 */
case class HistogramEntry(frequency: Long, colorHex: String, rgb: MRgb)

/**
 * Обертка для коллекции с данными гистограммы. Полезна при передаче гистограммы
 * с помощью акторов.
 * @param sorted Отсортированная гистограмма.
 */
case class Histogram(sorted: List[HistogramEntry])
