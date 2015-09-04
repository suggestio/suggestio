package io.suggest.sc.sjs.vm.wc

import io.suggest.adv.ext.model.im.ISize2di
import io.suggest.sjs.common.vm.style.{StyleWidth, StyleHeight}
import org.scalajs.dom.raw.HTMLImageElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.15 13:30
 * Description: Общая утиль для картинок welcome-карточек.
 */
trait WcImgUtil extends StyleWidth with StyleHeight {

  override type T <: HTMLImageElement

  /** Выставить параметры welcome-картинки в тег картинки. */
  protected def setImageWhMargin(wh: ISize2di, marginTopPx: Int): Unit = {
    setHeightPx( wh.height )
    setWidthPx( wh.width )
    // выставить margin
    val s = _underlying.style
    s.marginLeft = (-wh.width / 2).px
    s.marginTop = marginTopPx.px
  }

}
