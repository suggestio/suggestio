package io.suggest.sc.sjs.v.vutil

import io.suggest.adv.ext.model.im.{ISize2di, Size2di}
import io.suggest.common.css.CssSzImplicits
import org.scalajs.dom.Element
import org.scalajs.dom.raw.HTMLImageElement
import io.suggest.sc.ScConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 13:34
 * Description:
 */

/** View-утиль для взаимодейтсвия с тегами картинок. */
object VImgUtil extends CssSzImplicits {

  // TODO Задействовать safeEl.

  /** Прочитать значение из аттрибута data-width. */
  def readDataWidth(el: Element) = VUtil.getIntAttribute(el, WIDTH_ATTR)

  /** Прочитать значение из аттрибута data-height. */
  def readDataHeight(el: Element) = VUtil.getIntAttribute(el, HEIGHT_ATTR)

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
    el.style.width = wh.width.px
    el.style.height = wh.height.px
  }

}

