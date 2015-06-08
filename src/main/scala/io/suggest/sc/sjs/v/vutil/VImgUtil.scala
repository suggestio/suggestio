package io.suggest.sc.sjs.v.vutil

import io.suggest.adv.ext.model.im.{ISize2di, Size2di}
import org.scalajs.dom.Element
import org.scalajs.dom.raw.HTMLImageElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 13:34
 * Description:
 */

/** View-утиль для взаимодейтсвия с тегами картинок. */
object VImgUtil {

  // TODO Задействовать safeEl.

  /** Прочитать значение из аттрибута data-width. */
  def readDataWidth(el: Element) = VUtil.getIntAttribute(el, "data-width")

  /** Прочитать значение из аттрибута data-height. */
  def readDataHeight(el: Element) = VUtil.getIntAttribute(el, "data-height")

  /** Прочитать данные о размерах из data-{width, height}. */
  def readDataWh(el: Element): Option[Size2di] = {
    for {
      width  <- readDataWidth(el)
      height <- readDataHeight(el)
    } yield {
      Size2di(width = width, height = height)
    }
  }

  /** Выставить новые отображаемые размеры для картинки и margin-left. */
  def setImageWh(el: HTMLImageElement, wh: ISize2di): Unit = {
    el.style.width = wh.width + "px"
    el.style.height = wh.height + "px"
  }

}

