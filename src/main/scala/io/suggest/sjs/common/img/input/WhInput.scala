package io.suggest.sjs.common.img.input

import io.suggest.common.geom.d2.ISize2di
import org.scalajs.jquery.JQuery

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.05.15 10:07
 * Description: Поддержка чтения 2D-размеров (ширины и длины) из тега input, задаваемого
 * черег поле whInput.
 */
trait WhInput extends ISize2di {

  /**
   * Инпут, откуда можно прочесть данные для запроса.
   * Нужно реализовывать через val или lazy val. *.
   */
  def whInput: JQuery

  /** Парсинг целого числа из аттрибута тега. */
  protected def parseIntAttr(name: String): Int = {
    // TODO Вынести названия аттрибутов в константы.
    whInput.attr("data-" + name)
      .toOption
      .filter { !_.isEmpty }
      .get
      .toInt
  }

  /** Ширина для кропа. */
  override def width = parseIntAttr("width")

  /** Высота для кропа. */
  override def height = parseIntAttr("height")

}
