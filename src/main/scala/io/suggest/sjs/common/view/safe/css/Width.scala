package io.suggest.sjs.common.view.safe.css

import io.suggest.sjs.common.view.safe.ISafe
import io.suggest.sjs.common.view.vutil.CssSzImplicits
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 18:48
 * Description: Поддержка безопасного чтения/записи style.width.
 */
trait Width extends ISafe with CssSzImplicits {

  override type T <: HTMLElement

  protected def setWidth(widthCss: String): Unit = {
    _underlying.style.width = widthCss
  }

  protected def setWidthPx(widthPx: Int): Unit = {
    setWidth(widthPx.px)
  }

}
