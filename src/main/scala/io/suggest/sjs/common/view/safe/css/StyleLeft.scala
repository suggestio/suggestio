package io.suggest.sjs.common.view.safe.css

import io.suggest.common.css.CssSzImplicits
import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.08.15 19:05
 * Description: Поддержка для быстрого выставления style.left.
 */
trait StyleLeft extends ISafe with CssSzImplicits {

  override type T <: HTMLElement

  protected def setLeft(left: String): this.type = {
    _underlying.style.left = left
    this
  }

  protected def setLeftPx(leftPx: Int): this.type = {
    setLeft(leftPx.px)
  }

}
